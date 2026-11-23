package org.fiware.tmforum.migration.postgresql;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Switches of the {@code replay} command.
 *
 * @param unorderedPaths path globs ({@code *} within a segment, {@code **} across) whose response lists are sets
 * @param ignored        JSON pointer patterns left out of the diff
 * @param externalHosts  hosts of external targets, whose references are compared by {@code id} and {@code href} only
 * @param includeWrites  also replay writes, which change both deployments
 */
record ReplayOptions(URI brokerBaseUrl, URI postgresBaseUrl, List<String> unorderedPaths, List<String> ignored,
		Set<String> externalHosts, boolean includeWrites, boolean dryRun) {

	private static final String ANY_SEGMENTS = "**";
	private static final String ANY_IN_SEGMENT = "*";

	ReplayOptions {
		unorderedPaths = unorderedPaths == null ? List.of() : List.copyOf(unorderedPaths);
		ignored = ignored == null ? List.of() : List.copyOf(ignored);
		externalHosts = externalHosts == null ? Set.of() : Set.copyOf(externalHosts);
		ignored.forEach(ResponseDiff::parsePointer);
	}

	/**
	 * The diff rules for a request path, without its query string.
	 */
	ResponseDiff.Rules rules(String path) {
		boolean unordered = unorderedPaths.stream().anyMatch(glob -> globPattern(glob).matcher(path).matches());
		return new ResponseDiff.Rules(unordered, ignored, externalHosts);
	}

	static Pattern globPattern(String glob) {
		StringBuilder regex = new StringBuilder();
		int i = 0;
		while (i < glob.length()) {
			if (glob.startsWith(ANY_SEGMENTS, i)) {
				regex.append(".*");
				i += ANY_SEGMENTS.length();
			} else if (glob.startsWith(ANY_IN_SEGMENT, i)) {
				regex.append("[^/]*");
				i += ANY_IN_SEGMENT.length();
			} else {
				int next = glob.indexOf(ANY_IN_SEGMENT, i);
				int end = next < 0 ? glob.length() : next;
				regex.append(Pattern.quote(glob.substring(i, end)));
				i = end;
			}
		}
		return Pattern.compile(regex.toString());
	}
}
