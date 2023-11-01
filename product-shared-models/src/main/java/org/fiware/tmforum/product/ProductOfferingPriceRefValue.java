package org.fiware.tmforum.product;

import java.util.ArrayList;
import java.util.List;

import org.fiware.tmforum.common.domain.ReferenceValue;

public class ProductOfferingPriceRefValue extends ReferenceValue{
    @Override
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of("product-offering-price"));
    }
}
