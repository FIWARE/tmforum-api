package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

/**
 * A base class used to define the invariant characteristics and behavior
 * of an InstalledSoftware.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = SoftwareSpecification.TYPE_SOFTWARE_SPECIFICATION)
public class SoftwareSpecification extends SoftwareResourceSpecification {

	public static final String TYPE_SOFTWARE_SPECIFICATION = "software-specification";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "numUsersMax") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "numUsersMax") }))
	private Integer numUsersMax;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "numberProcessActiveTotal") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "numberProcessActiveTotal") }))
	private Integer numberProcessActiveTotal;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "softwareSupportPackage") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "softwareSupportPackage", targetClass = SoftwareSupportPackageRef.class) }))
	private SoftwareSupportPackageRef softwareSupportPackage;

	/**
	 * Create a new SoftwareSpecification.
	 *
	 * @param id the entity id
	 */
	public SoftwareSpecification(String id) {
		super(TYPE_SOFTWARE_SPECIFICATION, id);
	}
}
