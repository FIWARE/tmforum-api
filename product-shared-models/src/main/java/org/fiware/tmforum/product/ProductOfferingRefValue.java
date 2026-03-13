package org.fiware.tmforum.product;

import java.util.ArrayList;
import java.util.List;

import org.fiware.tmforum.common.domain.ReferenceValue;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = ProductOffering.TYPE_PRODUCT_OFFERING)
public class ProductOfferingRefValue extends ReferenceValue {

    @Override
    public List<String> getReferencedTypes() {
        // Jackson throws Unsupported Operation exeception in this list
        return new ArrayList<>(List.of(ProductOffering.TYPE_PRODUCT_OFFERING));
    }
}
