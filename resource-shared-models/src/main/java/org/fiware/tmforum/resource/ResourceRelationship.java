package org.fiware.tmforum.resource;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.validation.ReferencedEntity;

@Data
public class ResourceRelationship {

    private String relationshipType;
    // needs to work as ref or value!
    private ResourceRef resource;
    private String atBaseType;
    private String atSchemaLocation;
    private String atType;
}
