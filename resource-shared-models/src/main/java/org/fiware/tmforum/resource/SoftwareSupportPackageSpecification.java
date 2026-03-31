package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import io.github.wistefan.mapping.annotations.MappingEnabled;

/**
 * A base class used to define the invariant characteristics and behavior
 * of a SoftwareSupportPackage. It is a sub-type of PhysicalResourceSpecification
 * with no additional fields.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = SoftwareSupportPackageSpecification.TYPE_SOFTWARE_SUPPORT_PACKAGE_SPECIFICATION)
public class SoftwareSupportPackageSpecification extends PhysicalResourceSpecification {

	public static final String TYPE_SOFTWARE_SUPPORT_PACKAGE_SPECIFICATION =
			"software-support-package-specification";

	/**
	 * Create a new SoftwareSupportPackageSpecification.
	 *
	 * @param id the entity id
	 */
	public SoftwareSupportPackageSpecification(String id) {
		super(TYPE_SOFTWARE_SUPPORT_PACKAGE_SPECIFICATION, id);
	}
}
