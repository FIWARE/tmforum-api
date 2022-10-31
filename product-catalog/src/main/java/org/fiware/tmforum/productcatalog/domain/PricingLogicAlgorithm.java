package org.fiware.tmforum.productcatalog.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class PricingLogicAlgorithm {

	private String id;
	private URI href;
	private String description;
	private String name;
	private String plaSpecId;
	private TimePeriod validFor;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
