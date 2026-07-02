package org.fiware.tmforum.common.querying;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.NgsiLdAttribute;
import io.github.wistefan.mapping.QueryAttributeType;
import io.github.wistefan.mapping.ReservedWordHandler;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.RelationshipObject;
import io.micronaut.context.annotation.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.QueryException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.github.wistefan.mapping.JavaObjectMapper.getGetterMethodByName;
import static org.fiware.tmforum.common.querying.Operator.GREATER_THAN;
import static org.fiware.tmforum.common.querying.Operator.GREATER_THAN_EQUALS;
import static org.fiware.tmforum.common.querying.Operator.LESS_THAN;
import static org.fiware.tmforum.common.querying.Operator.LESS_THAN_EQUALS;
import static org.fiware.tmforum.common.querying.Operator.REGEX;

@Slf4j
@Bean
@RequiredArgsConstructor
public class QueryParser {

    /**
     * JSON-LD reserved keywords carried in TMF payloads are persisted on
     * {@link org.fiware.tmforum.common.domain.Entity} under sanitised
     * NGSI-LD attribute names because NGSI-LD reserves the {@code @}-prefixed
     * forms structurally. To let clients filter by the natural TMF JSON name
     * (e.g. {@code ?@type=BlueprintProductSpecification}), we rewrite the
     * query path segments to the persisted internal names before resolving
     * the attribute against the target class.
     *
     * The {@code @id} entry maps to the unprefixed {@code id}, which is then
     * picked up by the single-segment {@code id} shortcut further down and
     * routed to NGSI-LD's native {@code id=} URL parameter — same end result
     * as a direct {@code ?id=...} query.
     */
    private static final Map<String, String> JSON_LD_RESERVED_TO_ENTITY_FIELD = Map.of(
            "@id", "id",
            "@type", "atType",
            "@baseType", "atBaseType",
            "@schemaLocation", "atSchemaLocation");

    private static List<String> translateJsonLdReservedTokens(List<String> pathParts) {
        return pathParts.stream()
                .map(p -> JSON_LD_RESERVED_TO_ENTITY_FIELD.getOrDefault(p, p))
                .toList();
    }

    protected final GeneralProperties generalProperties;

    // Keys for the "well-known" fields
    public static final String OFFSET_KEY = "offset";
    public static final String LIMIT_KEY = "limit";
    public static final String FIELDS_KEY = "fields";
    public static final String SORT_KEY = "sort";

    public static final String NGSI_LD_AND = ";";

    // NGSI-LD's own not-exists prefix operator (e.g. `!relatedParty.datasetId`), reused verbatim
    // as the sentinel QueryPart#operator() value for a "not exists" term (QueryPart#value() is
    // null in that case).
    public static final String NOT_EXISTS_PREFIX = "!";

    // the "," in tm-forum values is an or
    public static final String TMFORUM_OR_VALUE = ",";

    // the ";" in tm-forum parameters is an or
    public static final String TMFORUM_OR_KEY = ";";
    public static final String TMFORUM_AND = "&";

    // TMForum marks a sort field as descending by prefixing it with "-", e.g. sort=name,-billDate
    private static final String SORT_DESCENDING_PREFIX = "-";

    // NGSI-LD's orderBy suffixes a field with ";desc" to sort descending; omitting it means ascending
    private static final String ORDER_BY_DESCENDING_SUFFIX = ";desc";

    /**
     * Translates the TMForum {@code sort} query parameter (comma-separated list of properties,
     * optionally prefixed with "-" for descending, e.g. {@code sort=name,-billDate}) into the
     * NGSI-LD {@code orderBy} syntax (comma-separated list of "property;direction" pairs, direction
     * defaulting to ascending when omitted, e.g. {@code orderBy=name,billDate;desc}).
     *
     * @param queryClass class used to resolve the sorted attributes to their NGSI-LD path
     * @param parameters the request's query parameters
     * @return the NGSI-LD {@code orderBy} value, or {@code null} if no {@code sort} was requested
     */
    public String toOrderBy(Class<?> queryClass, Map<String, List<String>> parameters) {
        List<String> sortValues = parameters.get(SORT_KEY);
        if (sortValues == null || sortValues.isEmpty()) {
            return null;
        }
        String orderBy = sortValues.stream()
                .flatMap(value -> Arrays.stream(value.split(TMFORUM_OR_VALUE)))
                .filter(sortField -> !sortField.isBlank())
                .map(sortField -> toOrderByPart(queryClass, sortField))
                .collect(Collectors.joining(TMFORUM_OR_VALUE));
        return orderBy.isBlank() ? null : orderBy;
    }

    private String toOrderByPart(Class<?> queryClass, String sortField) {
        boolean descending = sortField.startsWith(SORT_DESCENDING_PREFIX);
        String attributeName = descending ? sortField.substring(1) : sortField;

        List<String> path = translateJsonLdReservedTokens(Arrays.asList(attributeName.split("\\.")));
        NgsiLdAttribute attribute = JavaObjectMapper.getNGSIAttributePath(path, queryClass);
        List<String> resolvedPath = new ArrayList<>(attribute.path().isEmpty()
                ? path.stream().map(ReservedWordHandler::escapeReservedWords).toList()
                : attribute.path());

        String first = resolvedPath.remove(0);
        String attrPath = first + String.join("", resolvedPath.stream().map(this::mapPathPart).toList());

        return descending ? attrPath + ORDER_BY_DESCENDING_SUFFIX : attrPath;
    }

    public static boolean hasFilter(Map<String, List<String>> values) {
        //remove the "non-filtering" keys
        values.remove(OFFSET_KEY);
        values.remove(LIMIT_KEY);
        values.remove(FIELDS_KEY);
        values.remove(SORT_KEY);
        // if something is left, we have filter
        return !values.isEmpty();
    }

    private static String removeWellKnownParameters(String queryString) {
        // Using linked list as the list returned by asList method is fixed-size
        // so the remove method raises a non implemented exception
        List<String> parameters = new LinkedList<>(Arrays.asList(queryString.split(TMFORUM_AND)));
        List<String> wellKnownParams = parameters
                .stream()
                .filter(p -> p.startsWith(LIMIT_KEY)
                        || p.startsWith(FIELDS_KEY)
                        || p.startsWith(OFFSET_KEY)
                        || p.startsWith(SORT_KEY)
                )
                .toList();
        // not part of the query
        parameters.removeAll(wellKnownParams);
        return String.join(TMFORUM_AND, parameters);
    }

    public QueryParams toNgsiLdQuery(Class<?> queryClass, String queryString) {
        queryString = removeWellKnownParameters(queryString);

        // NGSI-LD's q= already defines AND-before-OR precedence for an un-parenthesized term
        // chain (ETSI GS CIM 009 §4.9), so mixing & and ; here does not need any grouping/
        // parenthesization logic on our side - we only need to preserve, for each pair of
        // adjacent parameters, which separator connected them, and translate them in the same
        // left-to-right order. A "run" is a maximal sequence of OR-connected parameters; runs
        // themselves are always AND-connected to each other by construction.
        List<ConnectedQueryPart> tokens = QueryTokenizer.tokenize(queryString);
        List<List<ConnectedQueryPart>> orRuns = splitIntoOrRuns(tokens);

        List<String> ids = new ArrayList<>();
        List<String> types = new ArrayList<>();

        List<String> runFragments = orRuns.stream()
                .map(run -> {
                    List<QueryPart> runParts = run.stream()
                            .map(cqp -> parseParameter(cqp.rawParameter()))
                            .toList();
                    List<QueryPart> combinedParts = runParts.size() > 1 ? combineOrRun(runParts) : runParts;
                    return translateRun(combinedParts, queryClass, ids, types);
                })
                .filter(fragment -> !fragment.isEmpty())
                .toList();

        String query = String.join(NGSI_LD_AND, runFragments);

        String idList = null;
        if (!ids.isEmpty()) {
            idList = String.join(",", ids);
        }
        String typeList = null;
        if (!types.isEmpty()) {
            typeList = String.join(",", types);
        }
        if (query.isEmpty()) {
            query = null;
        }
        return new QueryParams(idList, typeList, query);
    }

    /**
     * Splits parameters into maximal contiguous runs of OR-connected parameters. A new run
     * starts every time an AND-connector is encountered; runs are therefore always AND-connected
     * to each other, and every parameter within a run is OR-connected to its neighbours.
     */
    private static List<List<ConnectedQueryPart>> splitIntoOrRuns(List<ConnectedQueryPart> tokens) {
        List<List<ConnectedQueryPart>> runs = new ArrayList<>();
        List<ConnectedQueryPart> currentRun = new ArrayList<>();
        for (ConnectedQueryPart token : tokens) {
            if (!currentRun.isEmpty() && token.connectorToPrevious() == LogicalOperator.AND) {
                runs.add(currentRun);
                currentRun = new ArrayList<>();
            }
            currentRun.add(token);
        }
        if (!currentRun.isEmpty()) {
            runs.add(currentRun);
        }
        return runs;
    }

    /**
     * Translates every QueryPart in an OR-run to its query-string fragment (resolving the
     * attribute path, routing id=/type= to the accumulators instead of the returned string) and
     * joins them with the configured OR key.
     */
    private String translateRun(List<QueryPart> runParts, Class<?> queryClass, List<String> ids, List<String> types) {
        String ngsidOrKey = generalProperties.getNgsildOrQueryKey();
        return runParts.stream()
                .map(qp -> translateQueryPart(qp, queryClass, ids, types))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(ngsidOrKey));
    }

    private String translateQueryPart(QueryPart qp, Class<?> queryClass, List<String> ids, List<String> types) {
        List<String> path = translateJsonLdReservedTokens(
                Arrays.asList(qp.attribute().split("\\.")));
        NgsiLdAttribute attribute = JavaObjectMapper.getNGSIAttributePath(
                path,
                queryClass);
        if (attribute.path().isEmpty()) {
            log.info("Attribute {} does not have a path in the base class. Get path to additional attributes.", qp.attribute());
            attribute = getPathToAdditionalAttributes(qp);
        }
        boolean isNotExists = qp.operator().equals(NOT_EXISTS_PREFIX);
        if (attribute.path().size() == 1 && attribute.path().contains("id")) {
            if (isNotExists) {
                throw new QueryException("!id is not supported, id always exists.");
            }
            ids.add(qp.value());
            return null;
        }
        if (attribute.path().size() == 1 && attribute.path().contains("type")) {
            if (isNotExists) {
                throw new QueryException("!type is not supported, type always exists.");
            }
            types.add(qp.value());
            return null;
        }

        return toQueryString(getQueryPart(attribute, qp, isRelationship(queryClass, attribute)), attribute.type());
    }

    /**
     * Groups the QueryParts of a single OR-run by attribute, the same way the whole query used
     * to be grouped when it was entirely OR-connected (see {@link #combineParts}), but scoped to
     * one run. Not-exists parts are excluded from grouping (combineParts joins .value() strings,
     * which would NPE/produce garbage since a not-exists part's value is null) and are appended
     * to the result untouched.
     */
    private List<QueryPart> combineOrRun(List<QueryPart> orRunParts) {
        List<QueryPart> notExists = orRunParts.stream()
                .filter(qp -> qp.operator().equals(NOT_EXISTS_PREFIX))
                .toList();
        List<QueryPart> combinable = orRunParts.stream()
                .filter(qp -> !qp.operator().equals(NOT_EXISTS_PREFIX))
                .toList();
        Map<String, List<QueryPart>> collectedParts = combinable.stream()
                .collect(Collectors.toMap(QueryPart::attribute, qp -> new ArrayList<>(List.of(qp)),
                        (qp1, qp2) -> {
                            qp1.addAll(qp2);
                            return qp1;
                        }));
        List<QueryPart> combined = new ArrayList<>(collectedParts.entrySet().stream()
                .flatMap(entry -> combineParts(entry.getKey(), entry.getValue()).stream())
                .toList());
        combined.addAll(notExists);
        return combined;
    }

    private NgsiLdAttribute getPathToAdditionalAttributes(QueryPart queryPart) {
        List<String> path = new ArrayList<>(
                Arrays.stream(queryPart.attribute().split("\\."))
                        .map(ReservedWordHandler::escapeReservedWords)
                        .toList());

        if (queryPart.operator().equals(NOT_EXISTS_PREFIX)) {
            // no value to type-sniff; NGSI-LD's not-exists check is type-agnostic. The returned
            // type is unused downstream for this case (toQueryString's not-exists branch never
            // calls encodeValue).
            return new NgsiLdAttribute(path, QueryAttributeType.STRING);
        }
        if (isBoolean(queryPart.value())) {
            return new NgsiLdAttribute(path, QueryAttributeType.BOOLEAN);
        }
        if (isNumber(queryPart.value())) {
            return new NgsiLdAttribute(path, QueryAttributeType.NUMBER);
        }
        return new NgsiLdAttribute(path, QueryAttributeType.STRING);

    }

    private static boolean isNumber(String theValue) {
        try {
            Double.parseDouble(theValue);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isBoolean(String theValue) {
        if (theValue.equals("true") || theValue.equals("false")) {
            return true;
        }
        return false;
    }

    private static boolean isRelationship(Class<?> queryClass, NgsiLdAttribute attribute) {
        log.debug("Is relationship? {}", attribute);

        Optional<Annotation> relevantAnnotation = getGetterMethodByName(queryClass, attribute.path().get(0))
                .flatMap(m -> Arrays.stream(m.getAnnotations()))
                .filter(AttributeGetter.class::isInstance)
                .filter(annotation -> (annotation instanceof AttributeGetter attributeGetter || annotation instanceof RelationshipObject))
                .findFirst();
        if (relevantAnnotation.isEmpty()) {
            return false;
        }
        return relevantAnnotation.map(annotation -> {
            if (annotation instanceof AttributeGetter attributeGetter) {
                return attributeGetter.value().equals(AttributeType.RELATIONSHIP)
                        || attributeGetter.value().equals(AttributeType.RELATIONSHIP_LIST);
            }
            if (annotation instanceof RelationshipObject) {
                return true;
            }
            return false;
        }).get();
    }

    private QueryPart getQueryPart(NgsiLdAttribute attribute, QueryPart qp, boolean isRel) {
        // The query part will depend on the type of query
        // if the query is to a relationship subproperties will be joined with .
        // if the query is to a property with structured values the path will be
        // added between brackets

        String attrPath;
        if (isRel) {
            attrPath = String.join(".", attribute.path());
            // remove .id, since it will be added in case of referenced entities
            if (attrPath.endsWith(".id")) {
                attrPath = attrPath.substring(0, attrPath.length() - 3);
            }
        } else {
            String first = attribute.path().remove(0);
            attrPath = first + String.join("", attribute.path()
                    .stream()
                    .map(this::mapPathPart)
                    .toList());
        }

        return new QueryPart(
                attrPath,
                qp.operator(),
                qp.value());
    }

    private String mapPathPart(String part) {
        if (generalProperties.getUseDotSeperator()) {
            return "." + part;
        } else {
            return "[" + part + "]";
        }
    }

    private String encodeValue(String value, QueryAttributeType type) {
        value = switch (type) {
            case STRING -> encodeStringValue(value);
            case BOOLEAN -> value;
            case NUMBER -> value;
        };
        return value;
    }

    private String encodeStringValue(String value) {
        String ngsildOrValue = generalProperties.getNgsildOrQueryValue();
        if (value.contains(ngsildOrValue)) {
            // remove the beginning ( and ending )
            // String noBraces = value.substring(1, value.length() - 1);
            String format = "(%s)";

            if (!generalProperties.getEncloseQuery()) {
                format = "%s";
            }

            return String.format(format, Arrays.stream(value.split(String.format("\\%s", ngsildOrValue)))
                    .map(v -> String.format("\"%s\"", v))
                    .collect(Collectors.joining(ngsildOrValue)));

        } else if (value.contains(NGSI_LD_AND)) {
            // remove the beginning ( and ending )
            //String noBraces = value.substring(1, value.length() - 1);
            return String.format("(%s)", Arrays.stream(value.split(String.format("\\%s", NGSI_LD_AND)))
                    .map(v -> String.format("\"%s\"", v))
                    .collect(Collectors.joining(NGSI_LD_AND)));
        } else {
            return String.format("\"%s\"", value);
        }
    }

    private List<QueryPart> combineParts(String attribute, List<QueryPart> uncombinedParts) {
        Map<String, List<QueryPart>> collectedParts = uncombinedParts.stream()
                .collect(
                        Collectors.toMap(QueryPart::operator, qp -> new ArrayList<>(List.of(qp)),
                                (qp1, qp2) -> {
                                    qp1.addAll(qp2);
                                    return qp1;
                                }));
        return collectedParts
                .entrySet()
                .stream()
                .map(entry -> {
                    String value = entry.getValue()
                            .stream()
                            .map(QueryPart::value)
                            .collect(Collectors.joining(TMFORUM_OR_VALUE));

                    return new QueryPart(attribute, entry.getKey(), value);
                })
                .collect(Collectors.toList());
    }

    private String toQueryString(QueryPart queryPart, QueryAttributeType queryAttributeType) {

        if (queryPart.operator().equals(NOT_EXISTS_PREFIX)) {
            return NOT_EXISTS_PREFIX + queryPart.attribute();
        }

        if (queryPart.value().contains(TMFORUM_OR_VALUE)) {
            String theQuery = "";
            List<String> encodedValues = new ArrayList<>(Arrays.stream(queryPart.value().split(TMFORUM_OR_VALUE))
                    .map(v -> encodeValue(v, queryAttributeType))
                    .toList());

            if (generalProperties.getIncludeAttributeInList()) {
                theQuery = encodedValues
                        .stream()
                        .map(v -> String.format("%s%s%s", queryPart.attribute(), queryPart.operator(), v))
                        .collect(Collectors.joining(generalProperties.getNgsildOrQueryValue()));
                if (generalProperties.getEncloseQuery()) {
                    return "(" + theQuery + ")";
                }
            } else {
                if (generalProperties.getEncloseQuery()) {
                    return String.format("%s%s(%s)", queryPart.attribute(), queryPart.operator(), encodedValues
                            .stream()
                            .collect(Collectors.joining(generalProperties.getNgsildOrQueryValue())));
                } else {
                    return String.format("%s%s%s", queryPart.attribute(), queryPart.operator(), encodedValues
                            .stream()
                            .collect(Collectors.joining(generalProperties.getNgsildOrQueryValue())));
                }
            }

            return theQuery;
        }

        return String.format("%s%s%s", queryPart.attribute(), queryPart.operator(), encodeValue(queryPart.value(), queryAttributeType));
    }

    private QueryPart paramsToQueryPart(String parameter, Operator operator) {
        String[] parameterParts = parameter.split(operator.getTmForumOperator().operator());
        if (parameterParts.length != 2) {
            throw new QueryException(String.format("%s is not a valid %s parameter.",
                    parameter,
                    operator.getTmForumOperator().operator()));
        }
        return new QueryPart(
                parameterParts[0],
                operator.getNgsiLdOperator(),
                parameterParts[1]);
    }

    private QueryPart getQueryFromEquals(String parameter) {

        // equals could also contain a textual operator, f.e. key.gt=value -> key>value
        Optional<Operator> containedOperator = getOperator(parameter);
        if (containedOperator.isEmpty()) {
            // its a plain equals
            return paramsToQueryPart(parameter, Operator.EQUALS);
        }

        QueryPart uncleanedQueryPart = paramsToQueryPart(parameter, Operator.EQUALS);
        String uncleanedAttribute = uncleanedQueryPart.attribute();
        String cleanAttribute = uncleanedAttribute.substring(0,
                uncleanedAttribute.length() - containedOperator.get().getTmForumOperator().textRepresentation()
                        .length());
        return new QueryPart(cleanAttribute, containedOperator.get().getNgsiLdOperator(), uncleanedQueryPart.value());

    }

    private QueryPart parseParameter(String parameter) {

        if (parameter.startsWith(NOT_EXISTS_PREFIX)) {
            return new QueryPart(parameter.substring(NOT_EXISTS_PREFIX.length()), NOT_EXISTS_PREFIX, null);
        }

        Operator operator = getOperatorFromParam(parameter);
        return switch (operator) {
            case GREATER_THAN -> paramsToQueryPart(parameter, GREATER_THAN);
            case GREATER_THAN_EQUALS -> paramsToQueryPart(parameter, GREATER_THAN_EQUALS);
            case LESS_THAN_EQUALS -> paramsToQueryPart(parameter, LESS_THAN_EQUALS);
            case LESS_THAN -> paramsToQueryPart(parameter, LESS_THAN);
            case REGEX -> paramsToQueryPart(parameter, REGEX);
            case EQUALS -> getQueryFromEquals(parameter);
        };

    }

    private static Operator getOperatorFromParam(String parameter) {
        if (parameter.contains(GREATER_THAN_EQUALS.getTmForumOperator().operator())) {
            return GREATER_THAN_EQUALS;
        }
        if (parameter.contains(Operator.LESS_THAN_EQUALS.getTmForumOperator().operator())) {
            return Operator.LESS_THAN_EQUALS;
        }
        if (parameter.contains(Operator.REGEX.getTmForumOperator().operator())) {
            return Operator.REGEX;
        }
        if (parameter.contains(GREATER_THAN.getTmForumOperator().operator())) {
            return GREATER_THAN;
        }
        if (parameter.contains(LESS_THAN.getTmForumOperator().operator())) {
            return LESS_THAN;
        }
        return Operator.EQUALS;
    }

    private static Optional<Operator> getOperator(String partToParse) {
        String[] parts = partToParse.split(Operator.EQUALS.getTmForumOperator().operator());
        return Arrays.stream(Operator.values())
                .filter(operator -> {
                    TMForumOperator tmForumOperator = operator.getTmForumOperator();
                    if (parts[0].endsWith(tmForumOperator.textRepresentation())) {
                        return true;
                    }
                    return false;
                })
                .findAny();
    }
}
