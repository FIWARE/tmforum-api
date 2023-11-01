package org.fiware.tmforum.product;

import java.util.ArrayList;
import java.util.List;

import org.fiware.tmforum.common.domain.ReferenceValue;

public class ProductOfferingRefValue extends ReferenceValue {

    @Override
    public List<String> getReferencedTypes() {
        // Jackson throws Unsupported Operation exeception in this list
        return new ArrayList<>(List.of("product-offering"));
    }
}
