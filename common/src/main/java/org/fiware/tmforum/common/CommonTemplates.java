package org.fiware.tmforum.common;

/**
 * Utility class to provide common templates
 */
public class CommonTemplates {

	private CommonTemplates() {
		//prevent instantiation
		throw new IllegalStateException("Utility class");
	}

	/**
	 * String template for an NGSI-LD compatible id
	 */
	public static final String ID_TEMPLATE = "urn:ngsi-ld:%s:%s";
}
