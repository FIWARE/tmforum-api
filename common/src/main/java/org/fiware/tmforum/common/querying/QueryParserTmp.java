package org.fiware.tmforum.common.querying;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.NgsiLdAttribute;
import io.github.wistefan.mapping.QueryAttributeType;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.common.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.querying.Operator.*;

public class QueryParserTmp {

	// Keys for the "well-known" fields
	public static final String OFFSET_KEY = "offset";
	public static final String LIMIT_KEY = "limit";
	public static final String FIELDS_KEY = "fields";
	public static final String FIELDS_SEPARATOR = ",";
	public static final String SORT_KEY = "sort";
	public static final String EVENT_TYPE_KEY = "eventType";

	public static final String NGSI_LD_OR = "|";
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
		List<String> parameters = new ArrayList<>(Arrays.asList(queryString.split(TMFORUM_AND)));
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

	public static String toNgsiLdQuery(Class<?> queryClass, String queryString) {
		LogicalOperator logicalOperator = getLogicalOperator(queryString);
		Stream<QueryPart> queryPartsStream = parseToQueryParts(queryString, logicalOperator);

		// translate the attributes
		Stream<String> queryStrings = queryPartsStream.map(qp -> {
					NgsiLdAttribute attribute = JavaObjectMapper.getNGSIAttributePath(
							Arrays.asList(qp.attribute().split("\\.")),
							queryClass);
					return new QueryPart(
							String.join(".", attribute.path()
							),
							qp.operator(),
							encodeValue(qp.value(), attribute.type()));
				})
				.map(QueryParserTmp::toQueryString);

		return switch (logicalOperator) {
			case AND -> queryStrings.collect(Collectors.joining(NGSI_LD_AND));
			case OR -> queryStrings.collect(Collectors.joining(NGSI_LD_OR));
		};
	}

	public static SubscriptionQuery parseNotificationQuery(String queryString, List<String> eventGroups) {
		SubscriptionQuery subscriptionQuery = new SubscriptionQuery();

		if (queryString == null || queryString.isEmpty()) {
			// TODO support for multiple event groups
			if (!eventGroups.isEmpty()) {
				subscriptionQuery.setEventGroupName(eventGroups.get(0));
			}
			return subscriptionQuery;
		}

		subscriptionQuery.setEventGroupName(getEventGroup(queryString));
		subscriptionQuery.setFields(getSelectionFields(queryString));
		subscriptionQuery.setEventTypes(getEventTypes(queryString));
		subscriptionQuery.setQuery(getQuery(queryString));

		return subscriptionQuery;
	}

	private static String getEventGroup(String queryString) {
		Set<String> eventGroupNames = new HashSet<>();
		Arrays.stream(queryString.split(TMFORUM_AND)).filter(p ->
				p.startsWith(EVENT_TYPE_KEY)).forEach(parameter -> {
					String eventType = parameter.split(Operator.EQUALS.getTmForumOperator().operator())[1];
					eventGroupNames.add(StringUtils.getEventGroupName(eventType));
				});
		if (eventGroupNames.size() != 1) {
			throw new RuntimeException("Events should belong to one entity");
		}
		return eventGroupNames.iterator().next();
	}

	private static List<String> getEventTypes(String queryString) {
		List<String> eventTypes = new ArrayList<>();
		Arrays.stream(queryString.split(TMFORUM_AND)).filter(p ->
				p.startsWith(EVENT_TYPE_KEY)).forEach(parameter -> {
			String eventType = parameter.split(Operator.EQUALS.getTmForumOperator().operator())[1];
			eventTypes.add(eventType);
		});
		return eventTypes;
	}

	private static String getQuery(String queryString) {
		List<String> parameters = new ArrayList<>(Arrays.asList(queryString.split(TMFORUM_AND)));
		List<String> toRemove = parameters
				.stream()
				.filter(p -> p.startsWith(LIMIT_KEY)
						|| p.startsWith(FIELDS_KEY)
						|| p.startsWith(OFFSET_KEY)
						|| p.startsWith(SORT_KEY)
						|| p.startsWith(EVENT_TYPE_KEY)
				)
				.toList();
		// not part of the query
		parameters.removeAll(toRemove);
		if (parameters.isEmpty()) {
			return "";
		}

		return String.join(TMFORUM_AND, parameters.stream().map(QueryParserTmp::truncateToEventPayload).toList());
	}

	private static List<String> getSelectionFields(String queryString) {
		Optional<String> optional = Arrays.stream(queryString.split(TMFORUM_AND)).filter(p ->
				p.startsWith(FIELDS_KEY)).findFirst();
        return optional.map(s -> Arrays.stream(s.split(FIELDS_SEPARATOR))
                .map(QueryParserTmp::truncateToEventPayload).toList()).orElse(null);
	}

	public static LogicalOperator getLogicalOperator(String queryString) {
		LogicalOperator logicalOperator = LogicalOperator.AND;
		// tmforum does not define queries combining AND and OR
		if (queryString.contains(TMFORUM_AND) && queryString.contains(TMFORUM_OR_KEY)) {
			throw new QueryException("Combining AND(&) and OR(;) on query level is not supported by the TMForum API.");
		}

		if (queryString.contains(TMFORUM_OR_KEY)) {
			logicalOperator = LogicalOperator.OR;
		}
		return logicalOperator;
	}

	public static Stream<QueryPart> parseToQueryParts(String queryString, LogicalOperator logicalOperator) {
		System.out.println(queryString);
		queryString = removeWellKnownParameters(queryString);
		List<String> parameters;

		if (logicalOperator == LogicalOperator.AND) {
			parameters = Arrays.asList(queryString.split(TMFORUM_AND));
		} else if (logicalOperator == LogicalOperator.OR) {
			parameters = Arrays.asList(queryString.split(TMFORUM_OR_KEY));
		} else {
			//query is just a single parameter query
			parameters = List.of(queryString);
		}

		Stream<QueryPart> queryPartsStream = parameters
				.stream()
				.map(QueryParserTmp::parseParameter);

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

	// query comes in format event.<object_name>.<object_attribute_path>, we need only <object_attribute_path>
	// e.g., event.product.category.name -> category.name
	private static String truncateToEventPayload(String attributePath) {
		String[] pathParts = attributePath.split("\\.", 3);
		if (pathParts.length < 3) {
			throw new RuntimeException();
		}
		return pathParts[2];
	}

	private static String encodeValue(String value, QueryAttributeType type) {
		return switch (type) {
			case STRING -> encodeStringValue(value);
			case BOOLEAN -> value;
			case NUMBER -> value;
		};
	}

	private static String encodeStringValue(String value) {
		if (value.contains(NGSI_LD_OR)) {
			// remove the beginning ( and ending )
			String noBraces = value.substring(1, value.length() - 1);
			return String.format("(%s)", Arrays.stream(noBraces.split(String.format("\\%s", NGSI_LD_OR)))
					.map(v -> String.format("\"%s\"", v))
					.collect(Collectors.joining(NGSI_LD_OR)));
		} else if (value.contains(NGSI_LD_AND)) {
			// remove the beginning ( and ending )
			String noBraces = value.substring(1, value.length() - 1);
			return String.format("(%s)", Arrays.stream(noBraces.split(String.format("\\%s", NGSI_LD_AND)))
					.map(v -> String.format("\"%s\"", v))
					.collect(Collectors.joining(NGSI_LD_AND)));
		} else {
			return String.format("\"%s\"", value);
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
							.collect(Collectors.joining(NGSI_LD_OR));
					if (entry.getValue().size() > 1) {
						value = String.format("(%s)", value);
					}
					return new QueryPart(attribute, entry.getKey(), value);
				})
				.collect(Collectors.toList());
	}

	private static String toQueryString(QueryPart queryPart) {
		return String.format("%s%s%s", queryPart.attribute(), queryPart.operator(), queryPart.value());
	}

	private static QueryPart paramsToQueryPart(String parameter, Operator operator) {
		String[] parameterParts = parameter.split(operator.getTmForumOperator().operator());
		if (parameterParts.length != 2) {
			throw new QueryException(String.format("%s is not a valid %s parameter.",
					parameter,
					operator.getTmForumOperator().operator()));
		}
		String value = parameterParts[1];
		if (value.contains(TMFORUM_OR_VALUE)) {
			value = String.format("(%s)", value.replace(TMFORUM_OR_VALUE, NGSI_LD_OR));
		}

		return new QueryPart(
				parameterParts[0],
				operator.getNgsiLdOperator(),
				value);
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
		return new QueryPart(cleanAttribute, containedOperator.get().getNgsiLdOperator(), uncleanedQueryPart.value());

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

	public static Operator getOperatorFromParam(String parameter) {
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
