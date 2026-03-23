package org.fiware.tmforum.resourceordering.domain;

import lombok.Data;

import java.net.URI;

/**
 * A generic reference value used for embedded references like appointments and resource specifications.
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
