package org.fiware.tmforum.mapping.desc.pojos.invalid;

import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.List;

@MappingEnabled(entityType = "my-pojo")
public class MyPojoWithPrivateId {
    private static final String ENTITY_TYPE = "my-pojo";

    private URI id;

    @EntityId
    private URI getId() {
        return URI.create("id");
    }

    @EntityType
    public String getType() {
        return ENTITY_TYPE;
    }

}
