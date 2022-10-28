package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntity;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntityWithWellKnown;

import java.net.URI;

@EqualsAndHashCode
@ToString
@MappingEnabled(entityType = "complex-pojo")
public class MyPojoWithSubEntityWellKnown {

    @Getter(onMethod = @__({@EntityId}))
    private URI id;

    @Getter(onMethod = @__({@EntityType}))
    private String type = "complex-pojo";

    public MyPojoWithSubEntityWellKnown(String id) {
        this.id = URI.create(id);
    }

    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "mySubProperty", fromProperties = true, targetClass = MySubPropertyEntityWithWellKnown.class)}))
    private MySubPropertyEntityWithWellKnown mySubProperty;
}
