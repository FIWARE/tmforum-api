package org.fiware.tmforum.common.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;
import java.util.List;

/**
 * Interface to indicate and provide the capability to reference other entities
 */
public interface ReferencedEntity {

	/**
	 * Type of entities that are allowed to be referenced.
	 *
	 * @return the types
	 */
	List<String> getReferencedTypes();

	/**
	 * Id of the entity itself
	 *
	 * @return the id
	 */
	@JsonIgnore
	URI getEntityId();

	/**
	 * Href of the referenced entity, used to classify it as external. Null when the reference has no href.
	 */
	@JsonIgnore
	default URI getReferenceHref() {
		return null;
	}
}
