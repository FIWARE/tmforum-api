package org.fiware.tmforum.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(ProductSpecification.TYPE_PRODUCT_SPECIFICATION));
    }

}
