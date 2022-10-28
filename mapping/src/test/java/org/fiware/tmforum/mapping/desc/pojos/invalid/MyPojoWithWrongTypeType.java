package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.AllArgsConstructor;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;

@AllArgsConstructor
@MappingEnabled(entityType = "my-pojo")
public class MyPojoWithWrongTypeType {

    private static final String ENTITY_TYPE = "my-pojo";

    private URI id;

    @EntityId
    public URI getId() {
        return id;
    }

    @EntityType
    public Integer getType() {
        return 3;
    }
}
