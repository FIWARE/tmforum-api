package org.fiware.tmforum.common;

/**
 * Utility class to provide common templates
 */
public class CommonTemplates {

	private CommonTemplates() {
		//prevent instantiation
		throw new IllegalStateException("Utility class");
	}

	public static final String ID_TEMPLATE = "urn:ngsi-ld:%s:%s";
}
