package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.common.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.querying.Operator.*;

public class SubscriptionQueryParser {

	// Keys for the "well-known" fields
	public static final String FIELDS_KEY = "fields";
	public static final String FIELDS_SEPARATOR = ",";
	public static final String EVENT_TYPE_KEY = "eventType";
	public static final String EVENT_PREFIX = "event.";

	// the ";" in tm-forum parameters is an or
	public static final String TMFORUM_OR_KEY = ";";
	public static final String TMFORUM_AND = "&";


	public static SubscriptionQuery parse(String queryString, List<String> defaultEventGroups) {
		SubscriptionQuery subscriptionQuery = new SubscriptionQuery();

		if (queryString != null && !queryString.isEmpty()) {
			List<String> parameters;
			LogicalOperator logicalOperator = LogicalOperator.AND;
			// tmforum does not define queries combining AND and OR
			if (queryString.contains(TMFORUM_AND) && queryString.contains(TMFORUM_OR_KEY)) {
				throw new QueryException("Combining AND(&) and OR(;) on query level is not supported by the TMForum API.");
			}
			if (queryString.contains(TMFORUM_AND)) {
				parameters = Arrays.asList(queryString.split(TMFORUM_AND));
            } else if (queryString.contains(TMFORUM_OR_KEY)) {
				parameters = Arrays.asList(queryString.split(TMFORUM_OR_KEY));
				logicalOperator = LogicalOperator.OR;
			} else {
				//query is just a single parameter query
				parameters = List.of(queryString);
			}

			Stream<QueryPart> queryPartsStream = parameters
					.stream()
					.map(SubscriptionQueryParser::parseParameter);

			List<String> queryParams = new ArrayList<>();
			queryPartsStream.forEach(qp -> {
				if (qp.attribute().equals(EVENT_TYPE_KEY)) {
					subscriptionQuery.addEventType(qp.value());
				} else if (qp.attribute().equals(FIELDS_KEY)) {
					subscriptionQuery.setFields(parseFields(qp.value()));
				} else {
					queryParams.add(removeEventPrefixFromAttributePath(qp.attribute()) + qp.operator() + qp.value());
				}
			});
			subscriptionQuery.setQuery(String.join(logicalOperator == LogicalOperator.AND ? TMFORUM_AND : TMFORUM_OR_KEY,
					queryParams));

			checkLogicalOperator(logicalOperator, subscriptionQuery);
		}

		if (subscriptionQuery.getEventTypes().isEmpty()) {
			subscriptionQuery.setEventTypes(
					defaultEventGroups.stream().flatMap(eventGroup ->
							EventConstants.ALLOWED_EVENT_TYPES.get(eventGroup).stream().map(
									eventType -> eventGroup + eventType)).toList());
		}

		subscriptionQuery.setEventGroups(subscriptionQuery.getEventTypes().stream()
				.map(StringUtils::getEventGroupName).collect(Collectors.toSet()));

		return subscriptionQuery;
	}

	private static void checkLogicalOperator(LogicalOperator logicalOperator, SubscriptionQuery subscriptionQuery) {
		if (logicalOperator == LogicalOperator.OR) {
			if (!subscriptionQuery.getFields().isEmpty()) {
				throw new QueryException("Logical operator OR(;) cannot be used with 'fields' selector");
			}
			if (!subscriptionQuery.getEventTypes().isEmpty() && !subscriptionQuery.getQuery().isEmpty()) {
				throw new QueryException("Logical operator OR(;) cannot be used when both 'eventType' and 'query' are defined");
			}
		} else {
			if (subscriptionQuery.getEventTypes().size() > 1) {
				throw new QueryException("Logical operator AND(&) cannot be used when several 'eventType' are defined");
			}
		}

	}

	private static List<String> parseFields(String fields) {
		return Arrays.stream(fields.split(FIELDS_SEPARATOR))
				.map(SubscriptionQueryParser::removeEventPrefixFromAttributePath).toList();
	}

	private static String removeEventPrefixFromAttributePath(String attributePath) {
		if (attributePath.startsWith(EVENT_PREFIX)) {
			return attributePath.substring(attributePath.indexOf(".") + 1);
		} else {
			return attributePath;
		}
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
		return new QueryPart(cleanAttribute, containedOperator.get().getTmForumOperator().operator(),
				uncleanedQueryPart.value());

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
}
