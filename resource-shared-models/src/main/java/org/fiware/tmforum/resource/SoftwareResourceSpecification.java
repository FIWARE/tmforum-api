package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Quantity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.List;

/**
 * An abstract base class used to define the invariant characteristics and behavior
 * of a SoftwareResource.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = SoftwareResourceSpecification.TYPE_SOFTWARE_RESOURCE_SPECIFICATION)
public class SoftwareResourceSpecification extends LogicalResourceSpecification {

	public static final String TYPE_SOFTWARE_RESOURCE_SPECIFICATION = "software-resource-specification";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "buildNumber") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "buildNumber") }))
	private String buildNumber;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "isDistributable") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "isDistributable") }))
	private Boolean isDistributable;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "isExperimental") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "isExperimental") }))
	private Boolean isExperimental;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "maintenanceVersion") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "maintenanceVersion") }))
	private String maintenanceVersion;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "majorVersion") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "majorVersion") }))
	private String majorVersion;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "minorVersion") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "minorVersion") }))
	private String minorVersion;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "otherDesignator") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "otherDesignator") }))
	private String otherDesignator;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "releaseStatus") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "releaseStatus") }))
	private String releaseStatus;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "installSize") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "installSize") }))
	private Quantity installSize;

	/**
	 * Create a new SoftwareResourceSpecification with the default entity type.
	 *
	 * @param id the entity id
	 */
	public SoftwareResourceSpecification(String id) {
		super(TYPE_SOFTWARE_RESOURCE_SPECIFICATION, id);
	}

	/**
	 * Protected constructor for sub-types to specify their own NGSI-LD entity type.
	 *
	 * @param type the NGSI-LD entity type
	 * @param id   the entity id
	 */
	protected SoftwareResourceSpecification(String type, String id) {
		super(type, id);
	}
}
