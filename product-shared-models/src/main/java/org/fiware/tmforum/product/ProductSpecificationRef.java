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
import io.github.wistefan.mapping.annotations.Ignore;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

import static org.fiware.tmforum.product.ProductSpecification.TYPE_PRODUCT_SPECIFICATION;

@MappingEnabled(entityType = TYPE_PRODUCT_SPECIFICATION)
@EqualsAndHashCode(callSuper = true)
public class ProductSpecificationRef extends RefEntity {

    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "version", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY, targetName = "version", fromProperties = true)}))
    private String version;

    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "targetProductSchema", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY, targetName = "targetProductSchema", fromProperties = true)}))
    private TargetProductSchema targetProductSchema;

    public ProductSpecificationRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(TYPE_PRODUCT_SPECIFICATION));
    }
}
