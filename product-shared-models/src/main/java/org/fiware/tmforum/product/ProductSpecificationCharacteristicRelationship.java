package org.fiware.tmforum.product;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;

import java.net.URI;
import java.util.List;

@Data
public class ProductSpecificationCharacteristicRelationship {

	private String id;
	private URI href;
	private Integer charSpecSeq;
	private String name;
	private String relationshipType;
	private TimePeriod validFor;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;

}
