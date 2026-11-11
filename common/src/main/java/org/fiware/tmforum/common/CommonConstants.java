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
	 * String template for the NGSI-LD Link header pointing at the JSON-LD context url
	 */
	public static final String JSON_LD_CONTEXT_LINK_TEMPLATE =
			"<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json";

	/**
	 * Name for the entities cache
	 */
	public static final String ENTITIES_CACHE_NAME = "entities";

	/**
	 * Name for the tmforum-subscriptions cache
	 */
	public static final String TMFORUM_SUBSCRIPTIONS_CACHE_NAME = "tmforum-subscriptions";

	/**
	 * Name for the subscriptions cache
	 */
	public static final String SUBSCRIPTIONS_CACHE_NAME = "subscriptions";

	/**
	 * Micronaut environment of the PostgreSQL storage; {@code common} beans that the adapter replaces exclude it.
	 */
	public static final String POSTGRESQL_ENVIRONMENT = "postgresql";

}
