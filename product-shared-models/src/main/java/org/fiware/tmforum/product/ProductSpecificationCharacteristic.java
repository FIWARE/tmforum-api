package org.fiware.tmforum.product;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.service.CharacteristicValueSpecification;

import java.net.URI;
import java.util.List;

@Data
public class ProductSpecificationCharacteristic {

	private String tmfId;
	private Boolean configurable;
	private String description;
	private Boolean extensible;
	private Boolean isUnique;
	private Integer maxCardinality;
	private Integer minCardinality;
	private String name;
	private String regex;
	// NGSI-LD attribute name deliberately differs from the TMForum field name ("valueType"): Coraine
	// rejects a Property literally named `valueType` with "'valueType' must be a string", treating it
	// as a reserved sub-attribute name even though it's just this TMForum characteristic's data type.
	// @JsonAlias keeps entities written under the old `valueType` key readable until they're next
	// updated - PATCH /attrs replaces this whole list attribute, so it self-heals on the next write.
	@JsonAlias("valueType")
	private String atValueType;
	private List<ProductSpecificationCharacteristicRelationship> productSpecCharRelationship;
	private List<CharacteristicValueSpecification> productSpecCharacteristicValue;
	private TimePeriod validFor;
	private String atValueSchemaLocation;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
