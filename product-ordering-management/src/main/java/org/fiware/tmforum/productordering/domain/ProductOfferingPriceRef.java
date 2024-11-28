package org.fiware.tmforum.productordering.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class ProductOfferingPriceRef extends RefEntity {

    public ProductOfferingPriceRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(getAtReferredType()));
    }
}
