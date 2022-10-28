package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.AllArgsConstructor;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;

@AllArgsConstructor
@MappingEnabled(entityType = "my-pojo")
public class MyPojoWithoutType {

    private URI id;

    @EntityId
    public URI getId() {
        return id;
    }
}
