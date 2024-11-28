package org.fiware.tmforum.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = ProductOffering.TYPE_PRODUCT_OFFERING)
public class ProductOfferingRef extends RefEntity {

    public ProductOfferingRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(ProductOffering.TYPE_PRODUCT_OFFERING));
    }
}
