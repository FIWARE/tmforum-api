package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

/**
 * A SoftwareSupportPackage represents the package acquired by a consumer from a software vendor.
 * It can be materialized as one or several files (data) downloaded online or copied on a physical support.
 * It is a sub-type of PhysicalResource with no additional fields.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = SoftwareSupportPackage.TYPE_SOFTWARE_SUPPORT_PACKAGE)
public class SoftwareSupportPackage extends PhysicalResource {

	public static final String TYPE_SOFTWARE_SUPPORT_PACKAGE = "software-support-package";

	/**
	 * Create a new SoftwareSupportPackage.
	 *
	 * @param id the entity id
	 */
	public SoftwareSupportPackage(String id) {
		super(TYPE_SOFTWARE_SUPPORT_PACKAGE, id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(TYPE_SOFTWARE_SUPPORT_PACKAGE));
	}
}
