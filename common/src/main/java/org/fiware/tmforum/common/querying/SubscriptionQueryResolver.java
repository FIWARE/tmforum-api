package org.fiware.tmforum.common.querying;

import io.micronaut.context.annotation.Bean;
import org.fiware.tmforum.common.exception.QueryException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.querying.Operator.*;

@Bean
public class SubscriptionQueryResolver {
    // the ";" in tm-forum parameters is an or
    private static final String TMFORUM_OR_KEY = ";";
    private static final String TMFORUM_AND = "&";

    private LogicalOperator logicalOperator;
    private Stream<QueryPart> queryPartsStream;
    private String payloadName;

    private void init(String queryString, String payloadName) {
        // tmforum does not define queries combining AND and OR
        if (queryString.contains(TMFORUM_AND) && queryString.contains(TMFORUM_OR_KEY)) {
            throw new QueryException("Combining AND(&) and OR(;) on query level is not supported by the TMForum API.");
        }

        this.payloadName = payloadName;
        logicalOperator = LogicalOperator.AND;

        List<String> parameters;
        if (queryString.contains(TMFORUM_AND)) {
            parameters = Arrays.asList(queryString.split(TMFORUM_AND));
        } else if (queryString.contains(TMFORUM_OR_KEY)) {
            logicalOperator = LogicalOperator.OR;
            parameters = Arrays.asList(queryString.split(TMFORUM_OR_KEY));
        } else {
            parameters = List.of(queryString);
        }

        queryPartsStream = parameters
                .stream()
                .map(parameter -> {
                    QueryPart queryPart;
                    if (parameter.contains(GREATER_THAN_EQUALS.getTmForumOperator().operator())) {
                        queryPart = paramsToQueryPart(parameter, GREATER_THAN_EQUALS);
                    } else if (parameter.contains(Operator.LESS_THAN_EQUALS.getTmForumOperator().operator())) {
                        queryPart = paramsToQueryPart(parameter, LESS_THAN_EQUALS);
                    } else if (parameter.contains(Operator.REGEX.getTmForumOperator().operator())) {
                        queryPart = paramsToQueryPart(parameter, REGEX);
                    } else if (parameter.contains(GREATER_THAN.getTmForumOperator().operator())) {
                        queryPart = paramsToQueryPart(parameter, GREATER_THAN);
                    } else if (parameter.contains(LESS_THAN.getTmForumOperator().operator())) {
                        queryPart = paramsToQueryPart(parameter, LESS_THAN);
                    } else {
                        queryPart = getQueryFromEquals(parameter);
                    }
                    return queryPart;
                });
    }

    public <T> boolean doesQueryMatchCreateEvent(String queryString, T entity, String payloadName) {
        if (queryString == null || queryString.isEmpty()) {
            return true;
        }

        init(queryString, payloadName);

        Stream<Boolean> results = queryPartsStream.map(qp -> {
            FieldData fieldData = getFieldData(entity, qp);
            if (!fieldData.exists()) {
                return false;
            }

            return matches(qp, fieldData);
        });

        return evaluateResult(results);
    }

    public <T> boolean doesQueryMatchUpdateEvent(String queryString, T entity, T oldState, String payloadName) {
        if (queryString == null || queryString.isEmpty()) {
            return !entity.equals(oldState);
        }

        init(queryString, payloadName);

        Stream<Boolean> results = queryPartsStream.map(qp -> {
            FieldData updatedFieldData = getFieldData(entity, qp);
            FieldData oldFieldData = getFieldData(oldState, qp);

            if (updatedFieldData.fieldValue != null && !updatedFieldData.fieldValue.equals(oldFieldData.fieldValue) ||
                    oldFieldData.fieldValue != null && !oldFieldData.fieldValue.equals(updatedFieldData.fieldValue)) {
                return matches(qp, updatedFieldData);
            } else {
                return false;
            }

        });

        return evaluateResult(results);
    }

    private boolean evaluateResult(Stream<Boolean> results) {
        return switch (logicalOperator) {
            case AND -> results.allMatch(r -> r);
            case OR -> results.anyMatch(r -> r);
        };
    }

    private <T> FieldData getFieldData(T entity, QueryPart qp) {
        Object fieldValue = null;
        Class<?> fieldType = null;
        try {
            String attributePath = qp.attribute();
            if (doesAttributeBelongToPayload(attributePath, payloadName)) {
                String result = attributePath.substring(attributePath.indexOf(".") + 1);
                Field field = entity.getClass().getDeclaredField(result);
                field.setAccessible(true);
                fieldValue = field.get(entity);
                fieldType = field.getType();
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        return new FieldData(fieldValue, fieldType);
    }

    private boolean doesAttributeBelongToPayload(String attributePath, String payloadName) {
        return attributePath.startsWith(payloadName + ".");
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
                operator.getTmForumOperator().operator(),
                parameterParts[1]);
    }

    private QueryPart getQueryFromEquals(String parameter) {
        // equals could also contain a textual operator, f.e. key.gt=value -> key>value
        String[] parts = parameter.split(Operator.EQUALS.getTmForumOperator().operator());
        Optional<Operator> containedOperator = Arrays.stream(Operator.values())
                .filter(operator -> {
                    TMForumOperator tmForumOperator = operator.getTmForumOperator();
                    return parts[0].endsWith(tmForumOperator.textRepresentation());
                })
                .findAny();
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

    private boolean matches(QueryPart qp, FieldData fieldData) {
        boolean result = false;
        if (Objects.equals(qp.operator(), GREATER_THAN.getTmForumOperator().operator())) {
            if (fieldData.fieldType.equals(Instant.class)) {
                result = ((Instant) fieldData.fieldValue).compareTo(Instant.parse(qp.value())) > 0;
            } else {
                result = Float.parseFloat(fieldData.fieldValue.toString()) > Float.parseFloat(qp.value());
            }
        } else if (Objects.equals(qp.operator(), GREATER_THAN_EQUALS.getTmForumOperator().operator())) {
            if (fieldData.fieldType.equals(Instant.class)) {
                result = ((Instant) fieldData.fieldValue).compareTo(Instant.parse(qp.value())) >= 0;
            } else {
                result = Float.parseFloat(fieldData.fieldValue.toString()) >= Float.parseFloat(qp.value());
            }
        } else if (Objects.equals(qp.operator(), LESS_THAN_EQUALS.getTmForumOperator().operator())) {
            if (fieldData.fieldType.equals(Instant.class)) {
                result = ((Instant) fieldData.fieldValue).compareTo(Instant.parse(qp.value())) <= 0;
            } else {
                result = Float.parseFloat(fieldData.fieldValue.toString()) <= Float.parseFloat(qp.value());
            }
        } else if (Objects.equals(qp.operator(), LESS_THAN.getTmForumOperator().operator())) {
            if (fieldData.fieldType.equals(Instant.class)) {
                result = ((Instant) fieldData.fieldValue).compareTo(Instant.parse(qp.value())) < 0;
            } else {
                result = Float.parseFloat(fieldData.fieldValue.toString()) < Float.parseFloat(qp.value());
            }
        } else if (Objects.equals(qp.operator(), EQUALS.getTmForumOperator().operator())) {
            if (fieldData.fieldType.equals(Instant.class)) {
                result = ((Instant) fieldData.fieldValue).compareTo(Instant.parse(qp.value())) == 0;
            } else {
                result = fieldData.fieldValue.equals(qp.value());
            }
        }
        return result;
    }

    private static class FieldData {
        Object fieldValue;
        Class<?> fieldType;

        FieldData(Object fieldValue, Class<?> fieldType) {
            this.fieldValue = fieldValue;
            this.fieldType = fieldType;
        }

        boolean exists() {
            return fieldType != null && fieldValue != null;
        }
    }

}
