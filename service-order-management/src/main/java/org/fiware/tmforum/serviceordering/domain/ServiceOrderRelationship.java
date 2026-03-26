package org.fiware.tmforum.serviceordering.domain;

import lombok.Data;

import java.net.URI;

/**
 * Describes a relationship between service orders.
 */
@Data
public class ServiceOrderRelationship {

	private String href;
	private String id;
	private String relationshipType;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
	private String atReferredType;
}
