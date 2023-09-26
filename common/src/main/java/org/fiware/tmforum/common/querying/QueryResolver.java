package org.fiware.tmforum.common.querying;

import io.micronaut.context.annotation.Bean;
import org.fiware.tmforum.common.exception.QueryException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.querying.Operator.*;

@Bean
public class QueryResolver {
    // the "," in tm-forum values is an or
    public static final String TMFORUM_OR_VALUE = ",";
    // the ";" in tm-forum parameters is an or
    public static final String TMFORUM_OR_KEY = ";";
    public static final String TMFORUM_AND = "&";

    public <T> boolean doesQueryMatchCreateEvent(String queryString, T entity, String payloadName) {
        if (queryString == null || queryString.isEmpty()) {
            return true;
        }

        LogicalOperator logicalOperator = getLogicalOperator(queryString);
        Stream<QueryPart> queryPartsStream = getQueryPartsStream(queryString, logicalOperator);

        Stream<Boolean> results = queryPartsStream.map(qp -> {
            if (!doesAttributeBelongsToPayload(qp.attribute(), payloadName)) {
                return false;
            }

            Object fieldValue;
            Class<?> fieldType;
            try {
                Field field = entity.getClass().getDeclaredField(removePayloadPrefixFromAttributePath(
                        payloadName, qp.attribute()));
                field.setAccessible(true);
                fieldValue = field.get(entity);
                fieldType = field.getType();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                return false;
            }

            return matches(qp, fieldValue, fieldType);

        });

        return switch (logicalOperator) {
            case AND -> results.allMatch(r -> r);
            case OR -> results.anyMatch(r -> r);
        };
    }

    public  <T> boolean doesQueryMatchUpdateEvent(String queryString, T entity, T oldState, String payloadName) {
        if (queryString == null || queryString.isEmpty()) {
            return !entity.equals(oldState);
        }

        LogicalOperator logicalOperator = getLogicalOperator(queryString);
        Stream<QueryPart> queryPartsStream = getQueryPartsStream(queryString, logicalOperator);

        // translate the attributes
        Stream<Boolean> results = queryPartsStream.map(qp -> {
            if (!doesAttributeBelongsToPayload(qp.attribute(), payloadName)) {
                return false;
            }

            Object fieldValue;
            Class<?> fieldType;
            try {
                Field field = entity.getClass().getDeclaredField(removePayloadPrefixFromAttributePath(
                        payloadName, qp.attribute()));
                field.setAccessible(true);
                fieldValue = field.get(entity);
                fieldType = field.getType();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                fieldValue = null;
                fieldType = null;
            }

            Object oldValue;
            try {
                Field field = oldState.getClass().getDeclaredField(removePayloadPrefixFromAttributePath(
                        payloadName, qp.attribute()));
                field.setAccessible(true);
                oldValue = field.get(oldState);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                oldValue = null;
            }

            if (fieldValue != null && !fieldValue.equals(oldValue)) {
                return matches(qp, fieldValue, fieldType);
            } else {
                return false;
            }

        });

        return switch (logicalOperator) {
            case AND -> results.allMatch(r -> r);
            case OR -> results.anyMatch(r -> r);
        };
    }

    private static boolean doesAttributeBelongsToPayload(String attributePath, String payloadName) {
        return attributePath.startsWith(payloadName + ".");
    }

    private static String removePayloadPrefixFromAttributePath(String payloadName, String attributePath) {
        if (doesAttributeBelongsToPayload(attributePath, payloadName)) {
            return attributePath.substring(attributePath.indexOf(".") + 1);
        } else {
            return attributePath;
        }
    }

    private static List<QueryPart> combineParts(String attribute, List<QueryPart> uncombinedParts) {
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
                    if (entry.getValue().size() > 1) {
                        value = String.format("(%s)", value);
                    }
                    return new QueryPart(attribute, entry.getKey(), value);
                })
                .collect(Collectors.toList());
    }

    private static QueryPart paramsToQueryPart(String parameter, Operator operator) {
        String[] parameterParts = parameter.split(operator.getTmForumOperator().operator());
        if (parameterParts.length != 2) {
            throw new QueryException(String.format("%s is not a valid %s parameter.",
                    parameter,
                    operator.getTmForumOperator().operator()));
        }

        return new QueryPart(
                parameterParts[0],
                operator.getTmForumOperator().operator(),
                parameterParts[1]);
    }

    private static QueryPart getQueryFromEquals(String parameter) {

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
        return new QueryPart(cleanAttribute, containedOperator.get().getTmForumOperator().operator(), uncleanedQueryPart.value());

    }

    private static QueryPart parseParameter(String parameter) {

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
                    return parts[0].endsWith(tmForumOperator.textRepresentation());
                })
                .findAny();
    }

    private static LogicalOperator getLogicalOperator(String queryString) {
        LogicalOperator logicalOperator = LogicalOperator.AND;

        // tmforum does not define queries combining AND and OR
        if (queryString.contains(TMFORUM_AND) && queryString.contains(TMFORUM_OR_KEY)) {
            throw new QueryException("Combining AND(&) and OR(;) on query level is not supported by the TMForum API.");
        }
        if (!queryString.contains(TMFORUM_AND) && queryString.contains(TMFORUM_OR_KEY)) {
            logicalOperator = LogicalOperator.OR;
        }

        return logicalOperator;
    }

    private static Stream<QueryPart> getQueryPartsStream(String queryString, LogicalOperator logicalOperator) {
        List<String> parameters;
        if (queryString.contains(TMFORUM_AND)) {
            parameters = Arrays.asList(queryString.split(TMFORUM_AND));
        } else if (queryString.contains(TMFORUM_OR_KEY)) {
            parameters = Arrays.asList(queryString.split(TMFORUM_OR_KEY));
        } else {
            //query is just a single parameter query
            parameters = List.of(queryString);
        }

        Stream<QueryPart> queryPartsStream = parameters
                .stream()
                .map(QueryResolver::parseParameter);

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
        return queryPartsStream;
    }

    private static Boolean matches(QueryPart qp, Object fieldValue, Class<?> fieldType) {
        return switch (getOperatorFromParam(qp.operator())) {
            case GREATER_THAN -> {
                if (fieldType.equals(Instant.class)) {
                    yield ((Instant) fieldValue).compareTo(Instant.parse(qp.value())) > 0;
                } else {
                    yield Float.parseFloat(fieldValue.toString()) > Float.parseFloat(qp.value());
                }
            }
            case GREATER_THAN_EQUALS -> {
                if (fieldType.equals(Instant.class)) {
                    yield ((Instant) fieldValue).compareTo(Instant.parse(qp.value())) >= 0;
                } else {
                    yield Float.parseFloat(fieldValue.toString()) >= Float.parseFloat(qp.value());
                }
            }
            case LESS_THAN_EQUALS -> {
                if (fieldType.equals(Instant.class)) {
                    yield ((Instant) fieldValue).compareTo(Instant.parse(qp.value())) <= 0;
                } else {
                    yield Float.parseFloat(fieldValue.toString()) <= Float.parseFloat(qp.value());
                }
            }
            case LESS_THAN -> {
                if (fieldType.equals(Instant.class)) {
                    yield ((Instant) fieldValue).compareTo(Instant.parse(qp.value())) < 0;
                } else {
                    yield Float.parseFloat(fieldValue.toString()) < Float.parseFloat(qp.value());
                }
            }
            case EQUALS -> {
                if (fieldType.equals(Instant.class)) {
                    yield ((Instant) fieldValue).compareTo(Instant.parse(qp.value())) == 0;
                } else {
                    yield fieldValue.equals(qp.value());
                }
            }
            default -> false;
        };
    }

}
