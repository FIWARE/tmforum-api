package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntity;

import java.net.URI;
import java.util.List;

@MappingEnabled(entityType = "my-pojo")
public class MyInvalidRelationshipPojo {
    
    private static final String ENTITY_TYPE = "my-pojo";

    private URI id;

    // required constructor
    public MyInvalidRelationshipPojo(String id) {
        this.id = URI.create(id);
    }

    @EntityId
    public URI getId() {
        return id;
    }

    @EntityType
    public String getType() {
        return ENTITY_TYPE;
    }

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "sub-entity")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "sub-entity", targetClass = MySubEntityWithoutRelationshipObject.class)}))
    private MySubEntityWithoutRelationshipObject myInvalidRelationshipPojo;
}
