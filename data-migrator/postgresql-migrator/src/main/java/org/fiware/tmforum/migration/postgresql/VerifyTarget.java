package org.fiware.tmforum.migration.postgresql;

import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What {@code verify} reads from the migrated store; {@link PostgresVerifyTarget} in production, a fake in unit tests.
 * Every read only considers current rows.
 */
interface VerifyTarget {

	/**
	 * Current rows per type.
	 */
	Map<String, Long> currentCounts();

	/**
	 * The top-level document keys per stored id; an id that is not stored is absent.
	 */
	Map<URI, Set<String>> topLevelKeys(Collection<URI> ids);

	/**
	 * The stored document text per stored id; an id that is not stored is absent.
	 */
	Map<URI, String> storedDocuments(Collection<URI> ids);

	/**
	 * The document the target writes for a domain object.
	 */
	ObjectNode document(Object domainEntity);

	/**
	 * Reads a stored document like the target's repository does. Throws on a mapping failure.
	 */
	Object domainEntity(String document, Class<?> domainClass);

	/**
	 * The rows holding a {@value RawAttributeCheck#ESCAPE_PREFIX} key at any depth: their total number and the first
	 * {@code maxExamples} ids.
	 */
	EscapedKeyRows rowsWithEscapedKeys(int maxExamples);

	/**
	 * Result of {@link #rowsWithEscapedKeys(int)}.
	 */
	record EscapedKeyRows(long count, List<String> examples) {
	}
}
