package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.AllArgsConstructor;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

@AllArgsConstructor
@MappingEnabled(entityType = "my-pojo")
public class MyPojoWithWrongIdType {

    private static final String ENTITY_TYPE = "my-pojo";

    @Setter
    private Integer id;

    @EntityId
    public Integer getId() {
        return id;
    }

    @EntityType
    public String getType() {
        return ENTITY_TYPE;
    }
}
