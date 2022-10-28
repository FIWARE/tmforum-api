package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.Builder;
import lombok.NoArgsConstructor;
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
import java.util.Optional;
import java.util.function.Consumer;
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

    public Consumer<String> attributeConsumer;
    public Consumer<List<String>> attributeListConsumer;
    public Consumer<MySubPropertyEntity> relationshipConsumer;
    public Consumer<List<MySubPropertyEntity>> relationshipListConsumer;

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

    @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")
    public void setName(String name) {
        Optional.ofNullable(attributeConsumer).ifPresent(consumer -> consumer.accept(name));
    }

    @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "propertyList")
    public void setPropertyList(List<String> names) {
        Optional.ofNullable(attributeListConsumer).ifPresent(consumer -> consumer.accept(names));
    }

    @AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "subEntity")
    public void setSubEntity(MySubPropertyEntity mySubPropertyEntity) {
        Optional.ofNullable(relationshipConsumer).ifPresent(consumer -> consumer.accept(mySubPropertyEntity));
    }

    @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "subEntityList")
    public void getSubEntityList(List<MySubPropertyEntity> mySubPropertyEntities) {
        Optional.ofNullable(relationshipListConsumer).ifPresent(consumer -> consumer.accept(mySubPropertyEntities));
    }

}
