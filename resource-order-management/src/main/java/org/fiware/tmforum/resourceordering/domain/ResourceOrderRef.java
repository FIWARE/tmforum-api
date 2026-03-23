package org.fiware.tmforum.resourceordering.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference to a ResourceOrder entity, used for validation of relationships.
 */
@EqualsAndHashCode(callSuper = true)
public class ResourceOrderRef extends RefEntity {

    public ResourceOrderRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(ResourceOrder.TYPE_RESOURCE_ORDER));
    }
}
