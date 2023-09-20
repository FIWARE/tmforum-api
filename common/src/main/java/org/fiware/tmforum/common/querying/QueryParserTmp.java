package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.exception.QueryException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.querying.Operator.*;

public class QueryParserTmp {

	// Keys for the "well-known" fields
	public static final String OFFSET_KEY = "offset";
	public static final String LIMIT_KEY = "limit";
	public static final String FIELDS_KEY = "fields";
	public static final String SORT_KEY = "sort";

	public static final String NGSI_LD_OR = "|";
	// the "," in tm-forum values is an or
	public static final String TMFORUM_OR_VALUE = ",";
	// the ";" in tm-forum parameters is an or
	public static final String TMFORUM_OR_KEY = ";";
	public static final String TMFORUM_AND = "&";

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
