package org.fiware.tmforum.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class PricingLogicAlgorithm {

	private String algorithmId;
	private URI href;
	private String description;
	private String name;
	private String plaSpecId;
	private TimePeriod validFor;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
