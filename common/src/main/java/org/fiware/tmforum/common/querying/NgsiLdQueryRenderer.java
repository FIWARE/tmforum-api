package org.fiware.tmforum.common.querying;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.NgsiLdAttribute;
import io.github.wistefan.mapping.QueryAttributeType;
import io.github.wistefan.mapping.ReservedWordHandler;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.QueryFilter.AllOf;
import org.fiware.tmforum.storage.query.QueryFilter.AnyOf;
import org.fiware.tmforum.storage.query.QueryFilter.Comparison;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Renders the backend-neutral query model into the NGSI-LD {@code q} and {@code orderBy} parameters, applying the
 * broker dialect configured in {@link GeneralProperties}.
 */
@Singleton
@RequiredArgsConstructor
public class NgsiLdQueryRenderer {

	private static final String NGSI_LD_AND = ";";
	private static final String ORDER_BY_SEPARATOR = ",";
	// NGSI-LD's orderBy suffixes a field with ";desc" to sort descending; omitting it means ascending
	private static final String ORDER_BY_DESCENDING_SUFFIX = ";desc";
	private static final String RELATIONSHIP_PATH_SEPARATOR = ".";
	private static final String ID = "id";
	// the referenced entity's id is implied by the relationship itself
	private static final String RELATIONSHIP_ID_SUFFIX = RELATIONSHIP_PATH_SEPARATOR + ID;

	// NGSI-LD's native "not equal" operator, used to rewrite the `.regex` "not equal" idiom below
	private static final String NGSI_LD_NOT_EQUALS = "!=";

	/**
	 * Idiom used to express "not equal to X" via a negative-lookahead regex: {@code ^(?!X$).*$}. When
	 * {@link GeneralProperties#isOptimizeRegexNotEquals()} is enabled, a {@code .regex} filter matching
	 * this idiom is rewritten to NGSI-LD's native {@code !=} instead of forwarding a broker-side pattern
	 * match, since not every broker's regex engine supports lookahead (e.g. Scorpio's PostgreSQL-backed
	 * POSIX regex doesn't).
	 */
	private static final Pattern NOT_EQUALS_REGEX_IDIOM = Pattern.compile("^\\^\\(\\?!(.+?)\\$\\)\\.\\*\\$$");

	/**
	 * Characters NGSI-LD's {@code q} reads as syntax even inside an unquoted pattern, and that quoting does not
	 * protect either: Orion-LD 1.9.0 and Scorpio 5.0.3 read {@code a|b} as "matches a, or attribute b exists".
	 */
	private static final Pattern REGEX_QUERY_SYNTAX = Pattern.compile("[|,;()]");

	private final GeneralProperties generalProperties;

	/**
	 * The NGSI-LD {@code q} for the query's filter, or null without one.
	 */
	public String renderFilter(EntityQuery query) {
		if (query.filter() == null) {
			return null;
		}
		return render(query.queryClass(), query.filter());
	}

	/**
	 * The NGSI-LD {@code orderBy} for the query's sort, or null without one.
	 */
	public String renderOrderBy(EntityQuery query) {
		if (query.sort().isEmpty()) {
			return null;
		}
		return query.sort().stream()
				.map(sortKey -> renderSortKey(query.queryClass(), sortKey))
				.collect(Collectors.joining(ORDER_BY_SEPARATOR));
	}

	private String render(Class<?> queryClass, QueryFilter filter) {
		return switch (filter) {
			case Comparison comparison -> renderComparison(queryClass, comparison);
			case AllOf allOf -> renderChildren(queryClass, allOf.filters(), NGSI_LD_AND);
			case AnyOf anyOf -> renderChildren(queryClass, anyOf.filters(), generalProperties.getNgsildOrQueryKey());
		};
	}

	private String renderChildren(Class<?> queryClass, List<QueryFilter> children, String separator) {
		return children.stream()
				.map(child -> child instanceof Comparison
						? render(queryClass, child)
						: "(" + render(queryClass, child) + ")")
				.collect(Collectors.joining(separator));
	}

	private String renderSortKey(Class<?> queryClass, SortKey sortKey) {
		AttributePath path = sortKey.attribute();
		// the orderBy has always addressed an undeclared key by its translated names (@type -> atType)
		NgsiLdAttribute attribute = toNgsiLdAttribute(queryClass, path.declared() ? path
				: new AttributePath(QueryParser.translateJsonLdReservedTokens(path.segments()), false));
		List<String> resolvedPath = new ArrayList<>(sortKey.attribute().declared()
				? escapePath(attribute.path(), QueryParser.isRelationship(queryClass, attribute))
				: attribute.path());
		String first = resolvedPath.remove(0);
		String attrPath = first + String.join("", resolvedPath.stream().map(this::mapPathPart).toList());
		return sortKey.descending() ? attrPath + ORDER_BY_DESCENDING_SUFFIX : attrPath;
	}

	private String renderComparison(Class<?> queryClass, Comparison comparison) {
		NgsiLdAttribute attribute = toNgsiLdAttribute(queryClass, comparison.attribute());
		String attrPath = toAttrPath(attribute, QueryParser.isRelationship(queryClass, attribute));

		String operator = Operator.of(comparison.operator()).getNgsiLdOperator();
		List<String> values = comparison.values();
		ValueType valueType = comparison.valueType();
		if (comparison.operator() == ComparisonOperator.REGEX) {
			Matcher notEquals = NOT_EQUALS_REGEX_IDIOM.matcher(values.getFirst());
			if (generalProperties.isOptimizeRegexNotEquals() && notEquals.matches()) {
				operator = NGSI_LD_NOT_EQUALS;
				values = List.of(notEquals.group(1));
				valueType = ValueType.STRING;
			} else {
				return attrPath + operator + toRegExp(values.getFirst());
			}
		}

		ValueType encodingType = valueType;
		List<String> encodedValues = values.stream().map(value -> encodeValue(value, encodingType)).toList();
		return renderValues(attrPath + operator, encodedValues);
	}

	// NGSI-LD's RegExp is unquoted: Orion-LD rejects a quoted pattern, Scorpio takes both
	private static String toRegExp(String pattern) {
		// rejecting is better than a q that silently matches other entities
		if (REGEX_QUERY_SYNTAX.matcher(pattern).find()) {
			throw new QueryException(String.format(
					"The regex %s cannot be sent to the NGSI-LD broker: it holds one of |,;() which its query language reads as syntax.",
					pattern));
		}
		return pattern;
	}

	private String renderValues(String attributeAndOperator, List<String> encodedValues) {
		if (encodedValues.size() == 1) {
			return attributeAndOperator + encodedValues.getFirst();
		}
		if (generalProperties.getIncludeAttributeInList()) {
			// each element is a whole comparison, so they are OR-ed like filters
			String query = encodedValues.stream()
					.map(value -> attributeAndOperator + value)
					.collect(Collectors.joining(generalProperties.getNgsildOrQueryKey()));
			return generalProperties.getEncloseQuery() ? "(" + query + ")" : query;
		}
		String valueList = String.join(generalProperties.getNgsildOrQueryValue(), encodedValues);
		return generalProperties.getEncloseQuery()
				? attributeAndOperator + "(" + valueList + ")"
				: attributeAndOperator + valueList;
	}

	/**
	 * Only string values are quoted: boolean and number values are literals in the NGSI-LD query language, which
	 * the model has already validated. A date-time is rendered like a string, as the brokers store it.
	 */
	private static String encodeValue(String value, ValueType valueType) {
		return switch (valueType) {
			case STRING, DATE_TIME -> "\"" + value + "\"";
			case NUMBER, BOOLEAN -> value;
		};
	}

	/**
	 * Resolves the TMForum path to the NGSI-LD attribute. An undeclared (extension) attribute keeps its names,
	 * escaped like unmapped properties are when persisted.
	 */
	private static NgsiLdAttribute toNgsiLdAttribute(Class<?> queryClass, AttributePath attributePath) {
		if (attributePath.declared()) {
			NgsiLdAttribute attribute = JavaObjectMapper.getNGSIAttributePath(attributePath.segments(), queryClass);
			if (!attribute.path().isEmpty()) {
				return attribute;
			}
		}
		return new NgsiLdAttribute(
				attributePath.segments().stream().map(ReservedWordHandler::escapeReservedWords).toList(),
				QueryAttributeType.STRING);
	}

	/**
	 * Relationship sub-paths are joined with dots and lose a trailing {@code .id}; a property with a structured
	 * value addresses its sub-attributes with {@link #mapPathPart}.
	 */
	private String toAttrPath(NgsiLdAttribute attribute, boolean isRelationship) {
		if (isRelationship) {
			String attrPath = String.join(RELATIONSHIP_PATH_SEPARATOR, escapePath(attribute.path(), true));
			if (attrPath.endsWith(RELATIONSHIP_ID_SUFFIX)) {
				attrPath = attrPath.substring(0, attrPath.length() - RELATIONSHIP_ID_SUFFIX.length());
			}
			return attrPath;
		}
		List<String> escapedPath = new ArrayList<>(escapePath(attribute.path(), false));
		String first = escapedPath.remove(0);
		return first + String.join("", escapedPath.stream().map(this::mapPathPart).toList());
	}

	/**
	 * Entities are persisted with NGSI-LD reserved names escaped (e.g. valueType -> tmfEscaped-valueType),
	 * so filter and sort paths have to use the same escaped name. Only the trailing "id" of a relationship
	 * and the top-level entity "id" are left alone; nested "id" keys of structured properties are persisted escaped.
	 */
	private static List<String> escapePath(List<String> path, boolean isRel) {
		List<String> escaped = new ArrayList<>(path.stream().map(ReservedWordHandler::escapeReservedWords).toList());
		int last = path.size() - 1;
		// a top-level "id" is the entity id and is never escaped
		boolean entityId = last == 0;
		if ((isRel || entityId) && last >= 0 && ID.equals(path.get(last))) {
			escaped.set(last, ID);
		}
		return escaped;
	}

	private String mapPathPart(String part) {
		if (generalProperties.getUseDotSeperator()) {
			return "." + part;
		}
		return "[" + part + "]";
	}
}
