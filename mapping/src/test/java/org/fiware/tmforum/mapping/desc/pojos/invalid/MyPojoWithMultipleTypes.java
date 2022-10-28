package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.AllArgsConstructor;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;

@AllArgsConstructor
@MappingEnabled(entityType = "my-pojo")
public class MyPojoWithMultipleTypes {


    private URI id;

    @EntityId
    public URI getId() {
        return id;
    }

    @EntityType
    public String getType1() {
        return "type-1";
    }

    @EntityType
    public String getType2() {
        return "type-2";
    }
}
