package org.fiware.tmforum.common.querying;

import io.micronaut.context.annotation.Bean;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.stream.Stream;

@Bean
public class QueryResolver {

    public  <T> boolean doesQueryMatchCreateEvent(T entity, String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return true;
        }

        LogicalOperator logicalOperator = QueryParserTmp.getLogicalOperator(queryString);
        Stream<QueryPart> queryPartsStream = QueryParserTmp.parseToQueryParts(queryString, logicalOperator);

        // translate the attributes
        Stream<Boolean> results = queryPartsStream.map(qp -> {
            Object fieldValue;
            Class<?> fieldType;
            try {
                Field field = entity.getClass().getDeclaredField(qp.attribute());
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

    public  <T> boolean doesQueryMatchUpdateEvent(T entity, T oldState, String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return !entity.equals(oldState);
        }

        LogicalOperator logicalOperator = QueryParserTmp.getLogicalOperator(queryString);
        Stream<QueryPart> queryPartsStream = QueryParserTmp.parseToQueryParts(queryString, logicalOperator);

        // translate the attributes
        Stream<Boolean> results = queryPartsStream.map(qp -> {
            Object fieldValue;
            Class<?> fieldType;
            try {
                Field field = entity.getClass().getDeclaredField(qp.attribute());
                field.setAccessible(true);
                fieldValue = field.get(entity);
                fieldType = field.getType();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                fieldValue = null;
                fieldType = null;
            }

            Object oldValue;
            try {
                Field field = oldState.getClass().getDeclaredField(qp.attribute());
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

    private static Boolean matches(QueryPart qp, Object fieldValue, Class<?> fieldType) {
        return switch (QueryParserTmp.getOperatorFromParam(qp.operator())) {
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
