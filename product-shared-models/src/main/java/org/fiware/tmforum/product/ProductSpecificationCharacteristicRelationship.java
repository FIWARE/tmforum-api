package org.fiware.tmforum.product;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.TimePeriod;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;

import java.net.URI;
import java.util.List;

@Data
public class ProductSpecificationCharacteristicRelationship {

	private String tmfId;
	private URI href;
	private Integer charSpecSeq;
	private String name;
	private String relationshipType;
	private TimePeriod validFor;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;

}
