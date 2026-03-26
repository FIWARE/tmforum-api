package org.fiware.tmforum.resource;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;
import java.util.List;

/**
 * A migration, substitution, dependency or exclusivity relationship
 * between/among resource specifications.
 */
@Data
public class ResourceSpecificationRelationship {

	private ResourceSpecificationRef id;
	private String href;
	private Integer defaultQuantity;
	private Integer maximumQuantity;
	private Integer minimumQuantity;
	private String name;
	private String relationshipType;
	private String role;
	private List<ResourceSpecificationCharacteristic> characteristic;
	private TimePeriod validFor;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
