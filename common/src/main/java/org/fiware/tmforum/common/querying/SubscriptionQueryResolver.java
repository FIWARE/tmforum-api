package org.fiware.tmforum.common.querying;

import io.micronaut.context.annotation.Bean;
import org.fiware.tmforum.common.exception.QueryException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.fiware.tmforum.common.querying.Operator.*;

@Bean
public class SubscriptionQueryResolver {

    private List<ConnectedQueryPart> connectedTokens;
    private List<QueryPart> queryParts;
    private String payloadName;

    private void init(String queryString, String payloadName) {
        this.payloadName = payloadName;
        // NGSI-LD's own AND-before-OR precedence (see QueryParser/SubscriptionQueryParser) makes
        // mixing & and ; safe to evaluate without any grouping logic - evaluateResult folds the
        // per-token results left-to-right, respecting each token's own recorded connector.
        connectedTokens = QueryTokenizer.tokenize(queryString);
        queryParts = connectedTokens.stream()
                .map(cqp -> parseParameter(cqp.rawParameter()))
                .toList();
    }

    private QueryPart parseParameter(String parameter) {
        if (parameter.startsWith(QueryParser.NOT_EXISTS_PREFIX)) {
            return new QueryPart(parameter.substring(QueryParser.NOT_EXISTS_PREFIX.length()),
                    QueryParser.NOT_EXISTS_PREFIX, null);
        }
        if (parameter.contains(GREATER_THAN_EQUALS.getTmForumOperator().operator())) {
            return paramsToQueryPart(parameter, GREATER_THAN_EQUALS);
        } else if (parameter.contains(Operator.LESS_THAN_EQUALS.getTmForumOperator().operator())) {
            return paramsToQueryPart(parameter, LESS_THAN_EQUALS);
        } else if (parameter.contains(Operator.REGEX.getTmForumOperator().operator())) {
            return paramsToQueryPart(parameter, REGEX);
        } else if (parameter.contains(GREATER_THAN.getTmForumOperator().operator())) {
            return paramsToQueryPart(parameter, GREATER_THAN);
        } else if (parameter.contains(LESS_THAN.getTmForumOperator().operator())) {
            return paramsToQueryPart(parameter, LESS_THAN);
        } else {
            return getQueryFromEquals(parameter);
        }
    }

    public <T> boolean doesQueryMatchCreateEvent(String queryString, T entity, String payloadName) {
        if (queryString == null || queryString.isEmpty()) {
            return true;
        }

        init(queryString, payloadName);

        List<Boolean> results = queryParts.stream()
                .map(qp -> resolveCreateMatch(qp, entity))
                .toList();

        return evaluateResult(connectedTokens, results);
    }

    public <T> boolean doesQueryMatchUpdateEvent(String queryString, T entity, T oldState, String payloadName) {
        if (queryString == null || queryString.isEmpty()) {
            return !entity.equals(oldState);
        }

        init(queryString, payloadName);

        List<Boolean> results = queryParts.stream()
                .map(qp -> resolveUpdateMatch(qp, entity, oldState))
                .toList();

        return evaluateResult(connectedTokens, results);
    }

    private <T> boolean resolveCreateMatch(QueryPart qp, T entity) {
        FieldData fieldData = getFieldData(entity, qp);
        if (qp.operator().equals(QueryParser.NOT_EXISTS_PREFIX)) {
            return !fieldData.exists();
        }
        if (!fieldData.exists()) {
            return false;
        }
        return matches(qp, fieldData);
    }

    private <T> boolean resolveUpdateMatch(QueryPart qp, T entity, T oldState) {
        FieldData updatedFieldData = getFieldData(entity, qp);
        if (qp.operator().equals(QueryParser.NOT_EXISTS_PREFIX)) {
            // Evaluated as a pure state predicate against the new state only - a field's
            // existence is not expected to flip off in practice, so no old/new transition
            // tracking is needed here, unlike the value-comparison branch below.
            return !updatedFieldData.exists();
        }
        FieldData oldFieldData = getFieldData(oldState, qp);
        if (updatedFieldData.fieldValue != null && !updatedFieldData.fieldValue.equals(oldFieldData.fieldValue) ||
                oldFieldData.fieldValue != null && !oldFieldData.fieldValue.equals(updatedFieldData.fieldValue)) {
            return matches(qp, updatedFieldData);
        } else {
            return false;
        }
    }

    /**
     * Folds the per-token results left-to-right respecting AND-before-OR precedence: accumulate
     * an AND-run, then OR the accumulated run-results together. Degenerates to today's allMatch
     * for a pure-AND query and anyMatch for a pure-OR query.
     */
    private boolean evaluateResult(List<ConnectedQueryPart> tokens, List<Boolean> results) {
        boolean orAccumulator = false;
        boolean andAccumulator = results.get(0);
        for (int i = 1; i < tokens.size(); i++) {
            if (tokens.get(i).connectorToPrevious() == LogicalOperator.AND) {
                andAccumulator = andAccumulator && results.get(i);
            } else {
                orAccumulator = orAccumulator || andAccumulator;
                andAccumulator = results.get(i);
            }
        }
        return orAccumulator || andAccumulator;
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
