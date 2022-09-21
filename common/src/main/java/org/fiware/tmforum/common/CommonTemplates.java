package org.fiware.tmforum.common;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

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
