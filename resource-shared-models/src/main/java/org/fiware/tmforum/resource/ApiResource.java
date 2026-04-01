package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

/**
 * An Application Program Interface (API) is a set of routines, protocols,
 * and tools for building software applications. It is a sub-type of SoftwareResource
 * with no additional fields.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = ApiResource.TYPE_API_RESOURCE)
public class ApiResource extends SoftwareResource {

	public static final String TYPE_API_RESOURCE = "api-resource";

	/**
	 * Create a new ApiResource.
	 *
	 * @param id the entity id
	 */
	public ApiResource(String id) {
		super(TYPE_API_RESOURCE, id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(TYPE_API_RESOURCE));
	}
}
