package org.fiware.tmforum.resourceordering.domain;

import lombok.Data;

import java.net.URI;

@Data
public class ExternalReference {

	private String id;

	// TMF652 ExternalId fields (current public API shape).
	private String entityType;
	private String owner;

	// Legacy ExternalReference fields retained so entities persisted before the TMF652 ExternalId migration remain readable.
	private URI href;
	private String externalReferenceType;
	private String name;

	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
