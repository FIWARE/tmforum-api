package org.fiware.tmforum.common;

/**
 * Utility class to provide common templates
 */
public class CommonConstants {

	private CommonConstants() {
		//prevent instantiation
		throw new IllegalStateException("Utility class");
	}


	public static final int DEFAULT_OFFSET = 0;
	public static final int DEFAULT_LIMIT = 10;

	/**
	 * String template for an NGSI-LD compatible id
	 */
	public static final String ID_TEMPLATE = "urn:ngsi-ld:%s:%s";

	/**
	 * Name for the entities cache
	 */
	public static final String ENTITIES_CACHE_NAME = "entities";

	/**
	 * Name for the subscriptions cache
	 */
	public static final String SUBSCRIPTIONS_CACHE_NAME = "subscriptions";

}
