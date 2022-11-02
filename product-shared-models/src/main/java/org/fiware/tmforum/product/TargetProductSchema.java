package org.fiware.tmforum.product;

import lombok.Data;

import java.net.URI;

@Data
public class TargetProductSchema {

	private URI atSchemaLocation;
	private String atType;
}
