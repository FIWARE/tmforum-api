package org.fiware.tmforum.common.querying;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.NgsiLdAttribute;
import io.github.wistefan.mapping.QueryAttributeType;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.RelationshipObject;
import io.micronaut.context.annotation.Bean;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.QueryException;

import javax.smartcardio.ATR;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    protected final GeneralProperties generalProperties;

    // Keys for the "well-known" fields
    public static final String OFFSET_KEY = "offset";
    public static final String LIMIT_KEY = "limit";
    public static final String FIELDS_KEY = "fields";
    public static final String SORT_KEY = "sort";

    public static final String NGSI_LD_AND = ";";

    // the "," in tm-forum values is an or
    public static final String TMFORUM_OR_VALUE = ",";

    // the ";" in tm-forum parameters is an or
    public static final String TMFORUM_OR_KEY = ";";
    public static final String TMFORUM_AND = "&";

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

    public String toNgsiLdQuery(Class<?> queryClass, String queryString) {
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

        // collect the or values to single entries if they use the same key
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

        // translate the attributes
        Stream<String> queryStrings = queryPartsStream.map(qp -> {
                    NgsiLdAttribute attribute = JavaObjectMapper.getNGSIAttributePath(
                            Arrays.asList(qp.attribute().split("\\.")),
                            queryClass);
                    if (attribute.path().isEmpty()) {
                        log.info("Attribute {} does not have a path.", qp.attribute());
                        return null;
                    }
                    return toQueryString(getQueryPart(attribute, qp, isRelationship(queryClass, attribute)), attribute.type());
                })
                .filter(Objects::nonNull);

        String ngsidOrKey = generalProperties.getNgsildOrQueryKey();
        return switch (logicalOperator) {
            case AND -> queryStrings.collect(Collectors.joining(NGSI_LD_AND));
            case OR -> queryStrings.collect(Collectors.joining(ngsidOrKey));
        };
    }

    private static boolean isRelationship(Class<?> queryClass, NgsiLdAttribute attribute) {
        log.warn("Is relationship? {}", attribute);

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

        log.warn("Is rel: {}", isRel);

        String attrPath;
        if (isRel) {
            attrPath = String.join(".", attribute.path());
        } else {
            String first = attribute.path().remove(0);
            attrPath = first + String.join("", attribute.path()
                    .stream()
                    .map(a -> "[" + a + "]")
                    .toList());
        }

        return new QueryPart(
                attrPath,
                qp.operator(),
                qp.value());
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
        String ngsildOrValue = generalProperties.getNgsildOrQueryValue();
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
                            .collect(Collectors.joining(ngsildOrValue));

                    return new QueryPart(attribute, entry.getKey(), value);
                })
                .collect(Collectors.toList());
    }

    private String toQueryString(QueryPart queryPart, QueryAttributeType queryAttributeType) {

        if (queryPart.value().contains(TMFORUM_OR_VALUE)) {
            return "(" + Arrays.stream(queryPart.value().split(TMFORUM_OR_VALUE))
                    .map(v -> encodeValue(v, queryAttributeType))
                    .map(v -> String.format("%s%s%s", queryPart.attribute(), queryPart.operator(), v))
                    .collect(Collectors.joining(generalProperties.getNgsildOrQueryValue())) + ")";
        }

        return String.format("%s%s%s", queryPart.attribute(), queryPart.operator(), encodeValue(queryPart.value(), queryAttributeType));
    }

    private QueryPart paramsToQueryPart(String parameter, Operator operator) {
        String ngsildOrValue = generalProperties.getNgsildOrQueryValue();
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
