package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.Data;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;

@Data
@MappingEnabled(entityType = "my-pojo")
public class MyPojoWithMultipleIds {

    private static final String ENTITY_TYPE = "my-pojo";

    private String myName;

    @EntityId
    public URI getId1() {
        return URI.create("id-1");
    }

    @EntityId
    public URI getId2() {
        return URI.create("id-2");
    }

    @EntityType
    public String getType() {
        return ENTITY_TYPE;
    }
}
