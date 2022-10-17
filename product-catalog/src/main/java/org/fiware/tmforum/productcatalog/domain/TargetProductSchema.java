package org.fiware.tmforum.productcatalog.domain;

import lombok.Data;

import java.net.URI;

@Data
public class TargetProductSchema {

	private URI atSchemaLocation;
	private String atType;
}
