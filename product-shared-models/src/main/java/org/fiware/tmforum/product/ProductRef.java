package org.fiware.tmforum.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.resource.Resource;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Product.TYPE_PRODUCT)
public class ProductRef extends RefEntity {

    public ProductRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(Product.TYPE_PRODUCT));
    }
}
