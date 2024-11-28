package org.fiware.tmforum.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@MappingEnabled(entityType = ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE)
@EqualsAndHashCode(callSuper = true)
public class ProductOfferingPriceRef extends RefEntity {

    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "version", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY, targetName = "version", fromProperties = true)}))
    private String version;

    public ProductOfferingPriceRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE));
    }
}
