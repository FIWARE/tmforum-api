package org.fiware.tmforum.serviceordering.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference to a ServiceOrder entity, used for validation of relationships.
 */
@EqualsAndHashCode(callSuper = true)
public class ServiceOrderRef extends RefEntity {

    /**
     * Creates a new service order reference with the given id.
     *
     * @param id the NGSI-LD id of the referenced service order
     */
    public ServiceOrderRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(ServiceOrder.TYPE_SERVICE_ORDER));
    }
}
