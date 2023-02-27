package org.fiware.tmforum.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.validation.ReferencedEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceRelationship extends Entity{

    private String relationshipType;
    // needs to work as ref or value!
    private ResourceRef resource;
}
