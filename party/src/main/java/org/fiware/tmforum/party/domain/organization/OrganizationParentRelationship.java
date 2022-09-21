package org.fiware.tmforum.party.domain.organization;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled
public class OrganizationParentRelationship extends OrganizationRelationship {

	public OrganizationParentRelationship(String id) {
		super(id);
	}
}
