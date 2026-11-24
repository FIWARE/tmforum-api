package org.fiware.tmforum.common.querying;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.NgsiLdAttribute;
import io.github.wistefan.mapping.QueryAttributeType;
import io.github.wistefan.mapping.ReservedWordHandler;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import io.github.wistefan.mapping.annotations.RelationshipObject;
import io.micronaut.context.annotation.Bean;
import io.micronaut.core.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.DateTimeLiterals;
import org.fiware.tmforum.storage.query.Literals;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.QueryFilter.AllOf;
import org.fiware.tmforum.storage.query.QueryFilter.AnyOf;
import org.fiware.tmforum.storage.query.QueryFilter.Comparison;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.wistefan.mapping.JavaObjectMapper.getGetterMethodByName;
import static io.github.wistefan.mapping.JavaObjectMapper.getSetterMethodByName;
import static org.fiware.tmforum.common.querying.Operator.GREATER_THAN;
import static org.fiware.tmforum.common.querying.Operator.GREATER_THAN_EQUALS;
import static org.fiware.tmforum.common.querying.Operator.LESS_THAN;
import static org.fiware.tmforum.common.querying.Operator.LESS_THAN_EQUALS;
import static org.fiware.tmforum.common.querying.Operator.REGEX;

/**
 * Parses TMForum filter and sort parameters into the backend-neutral query model, including all validation, so
 * every backend answers the same 400s. {@link NgsiLdQueryRenderer} renders the model for the brokers.
 */
@Slf4j
@Bean
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

    static List<String> translateJsonLdReservedTokens(List<String> pathParts) {
        return pathParts.stream()
                .map(p -> JSON_LD_RESERVED_TO_ENTITY_FIELD.getOrDefault(p, p))
                .toList();
    }

    // not read since the parser stopped splitting on ngsildOrQueryValue; kept so the constructor stays stable
    protected final GeneralProperties generalProperties;

    // Keys for the "well-known" fields
    public static final String OFFSET_KEY = "offset";
    public static final String LIMIT_KEY = "limit";
    public static final String FIELDS_KEY = "fields";
    public static final String SORT_KEY = "sort";

    // the "," in tm-forum values is an or
    public static final String TMFORUM_OR_VALUE = ",";

    // the ";" in tm-forum parameters is an or
    public static final String TMFORUM_OR_KEY = ";";
    public static final String TMFORUM_AND = "&";

    // TMForum marks a sort field as descending by prefixing it with "-", e.g. sort=name,-billDate
    private static final String SORT_DESCENDING_PREFIX = "-";

    private static final String PATH_SEPARATOR_REGEX = "\\.";
    private static final String ID = "id";
    private static final String TYPE = "type";

    /**
     * The operators comparing by order, as opposed to the ones comparing by equality. They require
     * the compared values to have an order, which booleans do not.
     */
    private static final Set<String> ORDERING_OPERATORS = Set.of(
            GREATER_THAN.getNgsiLdOperator(),
            GREATER_THAN_EQUALS.getNgsiLdOperator(),
            LESS_THAN.getNgsiLdOperator(),
            LESS_THAN_EQUALS.getNgsiLdOperator());

    private static final Set<Class<?>> DATE_TIME_CLASSES = Set.of(
            Instant.class, OffsetDateTime.class, ZonedDateTime.class, Date.class);

    public QueryParser(GeneralProperties generalProperties) {
        this.generalProperties = generalProperties;
    }

    /**
     * Parses the TMForum {@code sort} parameter (comma-separated properties, each optionally prefixed with "-" for
     * descending, e.g. {@code sort=name,-billDate}); empty if none was requested.
     */
    public List<SortKey> parseSort(Class<?> queryClass, Map<String, List<String>> parameters) {
        List<String> sortValues = parameters.get(SORT_KEY);
        if (sortValues == null || sortValues.isEmpty()) {
            return List.of();
        }
        return sortValues.stream()
                .flatMap(value -> Arrays.stream(value.split(TMFORUM_OR_VALUE)))
                .filter(sortField -> !sortField.isBlank())
                .map(sortField -> toSortKey(queryClass, sortField))
                .toList();
    }

    private static SortKey toSortKey(Class<?> queryClass, String sortField) {
        boolean descending = sortField.startsWith(SORT_DESCENDING_PREFIX);
        String attributeName = descending ? sortField.substring(SORT_DESCENDING_PREFIX.length()) : sortField;

        List<String> rawPath = Arrays.asList(attributeName.split(PATH_SEPARATOR_REGEX));
        List<String> path = translateJsonLdReservedTokens(rawPath);
        requireNonBlankSegments(sortField, path);
        NgsiLdAttribute attribute = JavaObjectMapper.getNGSIAttributePath(path, queryClass);
        if (attribute.path().isEmpty()) {
            // as in filters, an extension attribute is stored under the names the client sent
            return new SortKey(new AttributePath(rawPath, false), null, descending);
        }
        ValueType valueType = isDateTime(queryClass, path) ? ValueType.DATE_TIME : toValueType(attribute.type());
        return new SortKey(new AttributePath(path, true), valueType, descending);
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

    /**
     * Parses a TMForum filter query string. Throws {@link QueryException} for anything the backends must answer
     * with a 400.
     */
    public ParsedQuery parse(Class<?> queryClass, String queryString) {
        queryString = removeWellKnownParameters(queryString);

        List<String> parameters;
        LogicalOperator logicalOperator = LogicalOperator.AND;
        // tm-forum does not define queries combining AND and OR
        if (queryString.contains(TMFORUM_AND) && queryString.contains(TMFORUM_OR_KEY)) {
            throw new QueryException("Combining AND(&) and OR(;) on query level is not supported by the TMForum API.");
        }
        if (queryString.contains(TMFORUM_AND)) {
            parameters = Arrays.asList(queryString.split(TMFORUM_AND));
            logicalOperator = LogicalOperator.AND;
        } else if (queryString.contains(TMFORUM_OR_KEY)) {
            parameters = Arrays.asList(queryString.split(TMFORUM_OR_KEY));
            logicalOperator = LogicalOperator.OR;
        } else {
            //query is just a single parameter query
            parameters = List.of(queryString);
        }

        Stream<QueryPart> queryPartsStream = parameters
                .stream()
                .map(this::parseParameter);

        // collect the or values to single entries if they use the same key; the HashMap order is the output order
        if (logicalOperator == LogicalOperator.OR) {
            Map<String, List<QueryPart>> collectedParts = queryPartsStream.collect(
                    Collectors.toMap(QueryPart::attribute, qp -> new ArrayList<>(List.of(qp)),
                            (qp1, qp2) -> {
                                qp1.addAll(qp2);
                                return qp1;
                            }));
            queryPartsStream = collectedParts.entrySet().stream()
                    .flatMap(entry -> combineParts(entry.getKey(), entry.getValue()).stream());
        }
        List<String> ids = new ArrayList<>();
        List<String> types = new ArrayList<>();
        List<QueryFilter> comparisons = queryPartsStream
                .map(qp -> toComparison(queryClass, qp, ids, types))
                .flatMap(Optional::stream)
                .toList();

        return new ParsedQuery(joinOrNull(ids), joinOrNull(types), combine(logicalOperator, comparisons));
    }

    @Nullable
    private static String joinOrNull(List<String> values) {
        return values.isEmpty() ? null : String.join(TMFORUM_OR_VALUE, values);
    }

    @Nullable
    private static QueryFilter combine(LogicalOperator logicalOperator, List<QueryFilter> filters) {
        if (filters.isEmpty()) {
            return null;
        }
        if (filters.size() == 1) {
            return filters.getFirst();
        }
        return switch (logicalOperator) {
            case AND -> new AllOf(filters);
            case OR -> new AnyOf(filters);
        };
    }

    /**
     * Resolves and validates one (combined) query part. The {@code id}/{@code type} shortcuts go to the given
     * lists instead of becoming a comparison.
     */
    private Optional<QueryFilter> toComparison(Class<?> queryClass, QueryPart qp, List<String> ids,
                                               List<String> types) {
        List<String> rawPath = Arrays.asList(qp.attribute().split(PATH_SEPARATOR_REGEX));
        List<String> path = translateJsonLdReservedTokens(rawPath);
        requireNonBlankSegments(qp.attribute(), path);
        NgsiLdAttribute attribute = JavaObjectMapper.getNGSIAttributePath(path, queryClass);
        boolean declared = !attribute.path().isEmpty();
        if (!declared) {
            log.info("Attribute {} does not have a path in the base class. Get path to additional attributes.", qp.attribute());
            attribute = getPathToAdditionalAttributes(qp);
            // an extension attribute is stored under the names the client sent, not under the Entity fields
            path = rawPath;
        }
        if (attribute.path().size() == 1 && attribute.path().contains(ID)) {
            ids.add(qp.value());
            return Optional.empty();
        }
        if (attribute.path().size() == 1 && attribute.path().contains(TYPE)) {
            types.add(qp.value());
            return Optional.empty();
        }

        validateOperator(qp, attribute.type());
        Operator operator = Operator.ofNgsiLdOperator(qp.operator());
        List<String> values = toValues(qp, operator, attribute.type());
        ValueType valueType = toValueType(attribute.type());
        if (declared && operator != REGEX && valueType == ValueType.STRING && isDateTime(queryClass, path)
                && values.stream().allMatch(value -> DateTimeLiterals.parse(value).isPresent())) {
            valueType = ValueType.DATE_TIME;
        }
        if (declared && path.size() == 1 && isRelationship(queryClass, attribute)) {
            // the model always points at the referenced entity's id
            path = Stream.concat(path.stream(), Stream.of(ID)).toList();
        }
        return Optional.of(new Comparison(new AttributePath(path, declared), operator.getComparisonOperator(),
                valueType, values));
    }

    /**
     * Splits the (combined) value into exact literals on {@link #TMFORUM_OR_VALUE}. A regex is never split.
     * Numbers and booleans are trimmed and validated.
     */
    private List<String> toValues(QueryPart qp, Operator operator, QueryAttributeType type) {
        if (operator == REGEX) {
            return List.of(qp.value());
        }
        Stream<String> values = Arrays.stream(qp.value().split(TMFORUM_OR_VALUE));
        List<String> literals = switch (type) {
            case STRING -> values.toList();
            case BOOLEAN -> values.map(value -> validateLiteral(qp.attribute(), value, Literals.BOOLEAN, "boolean"))
                    .toList();
            case NUMBER -> values.map(value -> validateLiteral(qp.attribute(), value, Literals.NUMBER, "number"))
                    .toList();
        };
        if (literals.isEmpty()) {
            throw new QueryException(String.format("%s has no value to compare with.", qp.attribute()));
        }
        return literals;
    }

    private static void requireNonBlankSegments(String attribute, List<String> path) {
        if (path.stream().anyMatch(String::isBlank)) {
            throw new QueryException(String.format("%s is not a valid attribute path.", attribute));
        }
    }

    private static ValueType toValueType(QueryAttributeType type) {
        return switch (type) {
            case STRING -> ValueType.STRING;
            case NUMBER -> ValueType.NUMBER;
            case BOOLEAN -> ValueType.BOOLEAN;
        };
    }

    /**
     * Whether the path ends at a declared field of a date-time Java type. Walks the getters the same way
     * {@link JavaObjectMapper#getNGSIAttributePath} does, since the mapping library reports dates as strings.
     */
    private static boolean isDateTime(Class<?> queryClass, List<String> path) {
        return resolveLeafClass(queryClass, path)
                .filter(leaf -> DATE_TIME_CLASSES.stream().anyMatch(dateTime -> dateTime.isAssignableFrom(leaf)))
                .isPresent();
    }

    private static Optional<Class<?>> resolveLeafClass(Class<?> tClass, List<String> path) {
        String current = path.getFirst();
        if (current.equals(ID)) {
            return Optional.empty();
        }
        if (!tClass.isAnnotationPresent(MappingEnabled.class)) {
            return resolvePlainLeafClass(tClass, path);
        }
        Optional<AttributeSetter> setter = getSetterMethodByName(tClass, current)
                .flatMap(m -> Arrays.stream(m.getAnnotations()))
                .filter(AttributeSetter.class::isInstance)
                .map(AttributeSetter.class::cast)
                .findFirst();
        Optional<Method> getter = getGetterMethodByName(tClass, current)
                .filter(m -> Arrays.stream(m.getAnnotations()).anyMatch(AttributeGetter.class::isInstance))
                .findFirst();
        if (setter.isEmpty() || getter.isEmpty()) {
            return Optional.empty();
        }
        Class<?> returnType = elementClass(getter.get().getGenericReturnType());
        if (path.size() == 1) {
            return Optional.of(returnType);
        }
        Class<?> targetClass = setter.get().targetClass() != Object.class ? setter.get().targetClass() : returnType;
        return resolveLeafClass(targetClass, path.subList(1, path.size()));
    }

    private static Optional<Class<?>> resolvePlainLeafClass(Class<?> tClass, List<String> path) {
        Class<?> currentClass = tClass;
        for (String segment : path) {
            Optional<Type> getterReturn = getGetterMethodByName(currentClass, segment).findAny()
                    .map(Method::getGenericReturnType);
            if (getterReturn.isPresent()) {
                currentClass = elementClass(getterReturn.get());
                continue;
            }
            try {
                currentClass = elementClass(currentClass.getField(segment).getGenericType());
            } catch (NoSuchFieldException e) {
                return Optional.empty();
            }
        }
        return Optional.of(currentClass);
    }

    /**
     * The class a path segment continues in: the element class of a collection or array, else the raw class.
     * An unresolvable type variable or wildcard gives {@code Object}, which is never a date-time.
     */
    private static Class<?> elementClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.isArray() ? clazz.getComponentType() : clazz;
        }
        if (type instanceof GenericArrayType arrayType) {
            return elementClass(arrayType.getGenericComponentType());
        }
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> raw) {
            if (Collection.class.isAssignableFrom(raw)) {
                return elementClass(parameterized.getActualTypeArguments()[0]);
            }
            return raw;
        }
        return Object.class;
    }

    /**
     * The type of an extension attribute is inferred from its combined value, before the value split, so
     * {@code ext=1,2} stays a string comparison. A Java-only number form ({@code +5}, {@code NaN}, {@code 1d})
     * is typed NUMBER and then fails the literal check with a 400, as before the model split.
     */
    private NgsiLdAttribute getPathToAdditionalAttributes(QueryPart queryPart) {
        List<String> path = new ArrayList<>(
                Arrays.stream(queryPart.attribute().split(PATH_SEPARATOR_REGEX))
                        .map(ReservedWordHandler::escapeReservedWords)
                        .toList());

        if (Literals.BOOLEAN.matcher(queryPart.value()).matches()) {
            return new NgsiLdAttribute(path, QueryAttributeType.BOOLEAN);
        }
        if (isJavaNumber(queryPart.value())) {
            return new NgsiLdAttribute(path, QueryAttributeType.NUMBER);
        }
        return new NgsiLdAttribute(path, QueryAttributeType.STRING);
    }

    private static boolean isJavaNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Whether the attribute's first NGSI-LD path segment is a relationship on the query class.
     */
    static boolean isRelationship(Class<?> queryClass, NgsiLdAttribute attribute) {
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

    /**
     * Rejects the ordering operators ({@code .gt}, {@code .gte}, {@code .lt}, {@code .lte}) for
     * boolean attributes, since there is no order to compare two booleans by. Brokers do not handle
     * such a query gracefully - Orion-LD 1.9.0 terminates on it - so it must not be forwarded.
     * Checked against the mapping library's type, which reports dates as strings, so {@code .regex} stays
     * allowed on them.
     *
     * @param queryPart          the query part to check
     * @param queryAttributeType type of the queried attribute on the queried class
     * @throws QueryException if the operator cannot be applied to the attribute's type
     */
    private static void validateOperator(QueryPart queryPart, QueryAttributeType queryAttributeType) {
        if (queryAttributeType == QueryAttributeType.BOOLEAN
                && ORDERING_OPERATORS.contains(queryPart.operator())) {
            throw new QueryException(String.format("%s cannot be compared with %s, it is a boolean attribute.",
                    queryPart.attribute(), queryPart.operator()));
        }
        if (queryPart.operator().equals(REGEX.getNgsiLdOperator()) && queryAttributeType != QueryAttributeType.STRING) {
            throw new QueryException(String.format("%s cannot be compared with %s, regex only applies to string attributes.",
                    queryPart.attribute(), queryPart.operator()));
        }
    }

    /**
     * @param attribute name of the queried attribute, only used for the error message
     * @param value     the value to check
     * @param pattern   literal syntax the value has to comply with
     * @param typeName  name of the expected type, only used for the error message
     * @return the value, trimmed of the surrounding whitespace the brokers ignore anyway
     * @throws QueryException if the value is not a valid literal of the expected type
     */
    private static String validateLiteral(String attribute, String value, Pattern pattern, String typeName) {
        String trimmedValue = value.trim();
        if (!pattern.matcher(trimmedValue).matches()) {
            throw new QueryException(
                    String.format("%s is not a valid %s value for attribute %s.", value, typeName, attribute));
        }
        return trimmedValue;
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

    private QueryPart paramsToQueryPart(String parameter, Operator operator) {
        // the operator symbol is split on literally - some symbols (e.g. REGEX's "*=") are not
        // valid regexes on their own and would otherwise throw a PatternSyntaxException
        String[] parameterParts = parameter.split(Pattern.quote(operator.getTmForumOperator().operator()));
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
