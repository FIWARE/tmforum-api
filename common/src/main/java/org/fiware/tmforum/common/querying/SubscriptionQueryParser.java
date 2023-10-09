package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.fiware.tmforum.common.querying.Operator.EQUALS;

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

			List<String> queryParams = new ArrayList<>();
			parameters.forEach(parameter -> {
				if (parameter.startsWith(EVENT_TYPE_KEY)) {
					subscriptionQuery.addEventType(getParamValue(parameter));
				} else if (parameter.startsWith(FIELDS_KEY)) {
					subscriptionQuery.setFields(parseFields(getParamValue(parameter)));
				} else {
					queryParams.add(removeEventPrefixFromAttributePath(parameter));
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

	private static String getParamValue(String parameter) {
		String[] parameterParts = parameter.split(EQUALS.getTmForumOperator().operator());
		if (parameterParts.length != 2) {
			throw new QueryException(String.format("%s is not a valid %s parameter.",
					parameter,
					EQUALS.getTmForumOperator().operator()));
		}

		return parameterParts[1];
	}
}
