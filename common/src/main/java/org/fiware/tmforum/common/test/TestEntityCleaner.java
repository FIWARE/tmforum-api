package org.fiware.tmforum.common.test;

/**
 * Removes every stored entity of a type, so each API IT starts from an empty store whatever the storage backend.
 */
public interface TestEntityCleaner {

	/**
	 * @param entityTypes one type, or several comma-separated as in NGSI-LD's {@code type} query parameter
	 */
	void deleteAll(String entityTypes);
}
