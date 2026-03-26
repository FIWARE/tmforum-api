package org.fiware.tmforum.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference to a SoftwareSupportPackage entity.
 */
@MappingEnabled(entityType = SoftwareSupportPackage.TYPE_SOFTWARE_SUPPORT_PACKAGE)
@EqualsAndHashCode(callSuper = true)
public class SoftwareSupportPackageRef extends RefEntity {

	/**
	 * Create a new SoftwareSupportPackageRef.
	 *
	 * @param id the entity id
	 */
	public SoftwareSupportPackageRef(@JsonProperty("id") String id) {
		super(id);
	}

	@Override
	@JsonIgnore
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(SoftwareSupportPackage.TYPE_SOFTWARE_SUPPORT_PACKAGE));
	}
}
