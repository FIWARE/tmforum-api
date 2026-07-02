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

	private enum TokenKind {
		EVENT_TYPE, FIELDS, QUERY
	}

	private record ClassifiedToken(ConnectedQueryPart token, TokenKind kind) {
	}

	public static SubscriptionQuery parse(String queryString, List<String> defaultEventGroups) {
		SubscriptionQuery subscriptionQuery = new SubscriptionQuery();

		if (queryString != null && !queryString.isEmpty()) {
			// NGSI-LD's q= already defines AND-before-OR precedence for an un-parenthesized term
			// chain, so mixing & and ; here does not require any grouping logic - we only need to
			// preserve, per adjacent pair of tokens, which separator connected them. Classification
			// (eventType/fields/query) is kept attached to each token so checkLogicalOperator can
			// inspect specific adjacencies instead of a single whole-query operator.
			List<ClassifiedToken> classifiedTokens = QueryTokenizer.tokenize(queryString).stream()
					.map(token -> new ClassifiedToken(token, classify(token.rawParameter())))
					.toList();

			List<ConnectedQueryPart> queryTokens = new ArrayList<>();
			classifiedTokens.forEach(classified -> {
				String parameter = classified.token().rawParameter();
				switch (classified.kind()) {
					case EVENT_TYPE -> subscriptionQuery.addEventType(getParamValue(parameter));
					case FIELDS -> subscriptionQuery.setFields(parseFields(getParamValue(parameter)));
					case QUERY -> queryTokens.add(new ConnectedQueryPart(
							removeEventPrefixFromAttributePath(parameter), classified.token().connectorToPrevious()));
				}
			});

			String theQuery = joinPreservingConnectors(queryTokens);
			if (!theQuery.isEmpty()) {
				subscriptionQuery.setQuery(theQuery);
			}

			checkLogicalOperator(classifiedTokens);
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

	private static TokenKind classify(String parameter) {
		if (parameter.startsWith(EVENT_TYPE_KEY)) {
			return TokenKind.EVENT_TYPE;
		}
		if (parameter.startsWith(FIELDS_KEY)) {
			return TokenKind.FIELDS;
		}
		return TokenKind.QUERY;
	}

	/**
	 * Re-joins the surviving query-bound tokens, preserving each token's own recorded connector
	 * to the previous *original* token - not "repaired" against the nearest surviving
	 * predecessor if eventType/fields tokens were filtered out in between. This is the simplest,
	 * most predictable rule, and the one today's single-separator behavior already implied.
	 */
	private static String joinPreservingConnectors(List<ConnectedQueryPart> queryTokens) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < queryTokens.size(); i++) {
			if (i > 0) {
				result.append(queryTokens.get(i).connectorToPrevious() == LogicalOperator.AND ? TMFORUM_AND : TMFORUM_OR_KEY);
			}
			result.append(queryTokens.get(i).rawParameter());
		}
		return result.toString();
	}

	/**
	 * Both remaining rules have a real technical grounding (unlike the removed "OR cannot combine
	 * with fields" rule, which had none): {@code eventType} membership is applied as a hard
	 * pre-filter at the broker/storage level, structurally separate from and prior to the content
	 * {@code query} match, so "OR between eventType and query" can never actually be honored by
	 * this architecture; and multiple {@code eventType=} values ANDed together can never match any
	 * real event (an event has exactly one type). Both are now checked per specific adjacency
	 * rather than "this operator was used somewhere in the whole query", so they only fire when
	 * the two conflicting tokens are actually connected to each other.
	 */
	private static void checkLogicalOperator(List<ClassifiedToken> classifiedTokens) {
		for (int i = 1; i < classifiedTokens.size(); i++) {
			ClassifiedToken previous = classifiedTokens.get(i - 1);
			ClassifiedToken current = classifiedTokens.get(i);
			LogicalOperator connector = current.token().connectorToPrevious();

			boolean isEventTypeQueryPair = (previous.kind() == TokenKind.EVENT_TYPE && current.kind() == TokenKind.QUERY)
					|| (previous.kind() == TokenKind.QUERY && current.kind() == TokenKind.EVENT_TYPE);
			if (connector == LogicalOperator.OR && isEventTypeQueryPair) {
				throw new QueryException("Logical operator OR(;) cannot be used when both 'eventType' and 'query' are defined");
			}

			boolean isEventTypePair = previous.kind() == TokenKind.EVENT_TYPE && current.kind() == TokenKind.EVENT_TYPE;
			if (connector == LogicalOperator.AND && isEventTypePair) {
				throw new QueryException("Logical operator AND(&) cannot be used when several 'eventType' are defined");
			}
		}
	}

	private static List<String> parseFields(String fields) {
		return Arrays.stream(fields.split(FIELDS_SEPARATOR))
				.map(SubscriptionQueryParser::removeEventPrefixFromAttributePath).toList();
	}

	private static String removeEventPrefixFromAttributePath(String attributePath) {
		// A leading "!" (not-exists) must not stop "event." from being recognised - strip it
		// first and reattach it once the event prefix has been removed.
		boolean notExists = attributePath.startsWith(QueryParser.NOT_EXISTS_PREFIX);
		String withoutNotExists = notExists
				? attributePath.substring(QueryParser.NOT_EXISTS_PREFIX.length())
				: attributePath;
		String withoutEventPrefix = withoutNotExists.startsWith(EVENT_PREFIX)
				? withoutNotExists.substring(withoutNotExists.indexOf(".") + 1)
				: withoutNotExists;
		return notExists ? QueryParser.NOT_EXISTS_PREFIX + withoutEventPrefix : withoutEventPrefix;
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
