package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.Data;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

@Data
@MappingEnabled(entityType = "my-pojo")
public class MyPojoWithoutId {

    private static final String ENTITY_TYPE = "my-pojo";

    private String myName;

    @EntityType
    public String getType() {
        return ENTITY_TYPE;
    }
}
