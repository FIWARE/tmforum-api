package org.fiware.tmforum.party.domain.organization;

import lombok.EqualsAndHashCode;
import io.github.wistefan.mapping.annotations.MappingEnabled;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Organization.TYPE_ORGANIZATION)
public class OrganizationChildRelationship extends OrganizationRelationship {

	public OrganizationChildRelationship(String id) {
		super(id);
	}

}
