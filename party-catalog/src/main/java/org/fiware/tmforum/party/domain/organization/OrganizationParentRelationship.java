package org.fiware.tmforum.party.domain.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import io.github.wistefan.mapping.annotations.MappingEnabled;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Organization.TYPE_ORGANIZATION)
public class OrganizationParentRelationship extends OrganizationRelationship {

    public OrganizationParentRelationship(@JsonProperty("id") String id) {
        super(id);
    }
}
