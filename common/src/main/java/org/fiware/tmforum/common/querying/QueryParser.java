package org.fiware.tmforum.common.querying;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.NgsiLdAttribute;
import io.github.wistefan.mapping.QueryAttributeType;
import org.fiware.tmforum.common.exception.QueryException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.querying.Operator.GREATER_THAN;
import static org.fiware.tmforum.common.querying.Operator.GREATER_THAN_EQUALS;
import static org.fiware.tmforum.common.querying.Operator.LESS_THAN;
import static org.fiware.tmforum.common.querying.Operator.LESS_THAN_EQUALS;
import static org.fiware.tmforum.common.querying.Operator.REGEX;

public class QueryParser {

	// Keys for the "well-known" fields
	public static final String OFFSET_KEY = "offset";
	public static final String LIMIT_KEY = "limit";
	public static final String FIELDS_KEY = "fields";
	public static final String SORT_KEY = "sort";

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
		List<String> parameters = Arrays.asList(queryString.split(TMFORUM_AND));
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

		queryString = removeWellKnownParameters(queryString);

		List<String> parameters;
		LogicalOperator logicalOperator = LogicalOperator.AND;
		// tmforum does not define queries combining AND and OR, therefor we
		// and case
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
				.map(QueryParser::parseParameter);

		// collect the or values to single entries if they use the same key
		if (logicalOperator == LogicalOperator.OR) {
			Map<String, List<QueryPart>> collectedParts = queryPartsStream.collect(
					Collectors.toMap(QueryPart::attribute, qp -> new ArrayList<QueryPart>(List.of(qp)),
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
					return new QueryPart(
							String.join(".", attribute.path()
							),
							qp.operator(),
							encodeValue(qp.value(), attribute.type()));
				})
				.map(QueryParser::toQueryString);

		return switch (logicalOperator) {
			case AND -> queryStrings.collect(Collectors.joining(NGSI_LD_AND));
			case OR -> queryStrings.collect(Collectors.joining(NGSI_LD_OR));
		};
	}

	private static String encodeValue(String value, QueryAttributeType type) {
		return switch (type) {
			case STRING -> String.format("\"%s\"", value);
			case BOOLEAN -> value;
			case NUMBER -> value;
		};
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
