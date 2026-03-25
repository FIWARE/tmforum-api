package org.fiware.tmforum.serviceordering.domain;

import lombok.Data;

import java.net.URI;

/**
 * A generic reference value used for embedded references like appointments and services.
 */
@Data
public class ReferenceValue {

	private String id;
	private URI href;
	private String name;
	private String atReferredType;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
