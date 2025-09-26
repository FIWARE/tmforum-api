package org.fiware.tmforum.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.annotations.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.fiware.tmforum.product.ProductSpecification.TYPE_PRODUCT_SPECIFICATION;

@MappingEnabled(entityType = TYPE_PRODUCT_SPECIFICATION)
@EqualsAndHashCode(callSuper = true)
public class ProductSpecificationRelationship extends RefEntity {

    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType", fromProperties = true)}))
    private String relationshipType;

    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor", fromProperties = true)}))
    private TimePeriod validFor;

    public ProductSpecificationRelationship(@JsonProperty("id") String id) {
        super(id);
    }

    @RelationshipObject
    @Override
    public URI getId() {
        return super.getId();
    }

    @JsonIgnore
    @DatasetId
    public URI getDatasetId() {
        String relType = Optional.ofNullable(getRelationshipType()).orElse("type");

        return URI.create(String.format("%s:%s", getId().toString(), relType));
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(ProductSpecification.TYPE_PRODUCT_SPECIFICATION));
    }

}
