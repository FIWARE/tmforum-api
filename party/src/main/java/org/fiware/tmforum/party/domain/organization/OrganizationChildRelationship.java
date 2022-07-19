package org.fiware.tmforum.party.domain.organization;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled
public class OrganizationChildRelationship extends OrganizationRelationship {

	public OrganizationChildRelationship(String id) {
		super(id);
	}

}
