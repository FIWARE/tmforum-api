package org.fiware.tmforum.resourceordering.domain;

import lombok.Data;

import java.net.URI;

/**
 * External reference of the individual or reference in another system.
 */
@Data
public class ExternalReference {

	private URI href;
	private String id;
	private String externalReferenceType;
	private String name;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
