package org.fiware.tmforum.common.querying;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a raw TMForum query string into tokens, preserving which separator ({@code &}=AND,
 * {@code ;}=OR) connected each token to the previous one. Unlike a plain {@code String.split},
 * this does not discard the separators, which is what lets callers translate a query that mixes
 * AND and OR without picking a single global {@link LogicalOperator} for the whole string.
 */
public final class QueryTokenizer {

	private static final Pattern SEPARATOR = Pattern.compile("([&;])");

	private QueryTokenizer() {
	}

	/**
	 * The first token's connector is {@link LogicalOperator#AND} by convention and must not be
	 * consulted by callers (it has no predecessor).
	 */
	public static List<ConnectedQueryPart> tokenize(String queryString) {
		List<ConnectedQueryPart> tokens = new ArrayList<>();
		Matcher matcher = SEPARATOR.matcher(queryString);
		int lastEnd = 0;
		LogicalOperator nextConnector = LogicalOperator.AND;
		while (matcher.find()) {
			String token = queryString.substring(lastEnd, matcher.start());
			tokens.add(new ConnectedQueryPart(token, nextConnector));
			nextConnector = matcher.group(1).equals("&") ? LogicalOperator.AND : LogicalOperator.OR;
			lastEnd = matcher.end();
		}
		tokens.add(new ConnectedQueryPart(queryString.substring(lastEnd), nextConnector));
		return tokens;
	}
}
