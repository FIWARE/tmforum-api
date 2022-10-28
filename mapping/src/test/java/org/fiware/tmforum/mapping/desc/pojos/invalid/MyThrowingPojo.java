package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntity;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Builder
@MappingEnabled(entityType = "my-pojo")
public class MyThrowingPojo {

    public Supplier<URI> idSupplier;
    public Supplier<String> typeSupplier;
    public Supplier<String> attributeSupplier;
    public Supplier<List<String>> attributeListSupplier;
    public Supplier<MySubPropertyEntity> relationshipSupplier;
    public Supplier<List<MySubPropertyEntity>> relationshipListSupplier;

    @EntityId
    public URI getId() {
        return Optional.ofNullable(idSupplier).map(Supplier::get).orElse(URI.create("urn:ngsi-ld:entity:entity"));
    }

    @EntityType
    public String getType() {
        return Optional.ofNullable(typeSupplier).map(Supplier::get).orElse("my-pojo");
    }

    @AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")
    public String getName() {
        return Optional.ofNullable(attributeSupplier).map(Supplier::get).orElse("name");
    }

    @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "propertyList")
    public List<String> getPropertyList() {
        return Optional.ofNullable(attributeListSupplier).map(Supplier::get).orElse(List.of("propertyList"));
    }

    @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "subEntity")
    public MySubPropertyEntity getSubEntity() {
        return Optional.ofNullable(relationshipSupplier).map(Supplier::get).orElse(new MySubPropertyEntity("urn:ngsi-ld:entity:entity"));
    }

    @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "subEntityList")
    public List<MySubPropertyEntity> getSubEntityList() {
        return Optional.ofNullable(relationshipListSupplier).map(Supplier::get).orElse(List.of(new MySubPropertyEntity("urn:ngsi-ld:entity:entity")));
    }
}
