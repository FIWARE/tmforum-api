package org.fiware.tmforum.mapping.desc.pojos.invalid;

import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;

@MappingEnabled(entityType = "my-pojo")
public class MyPojoWithPrivateType {
    private static final String ENTITY_TYPE = "my-pojo";

    private URI id;

    @EntityId
    public URI getId() {
        return URI.create("id");
    }

    @EntityType
    private String getType() {
        return ENTITY_TYPE;
    }

}
