package org.fiware.tmforum.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class AssociationSpecificationRef extends RefEntity {

    public AssociationSpecificationRef(@JsonProperty("id") URI id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        // maybe there is a concrete type in the future
        return new ArrayList<>(List.of(getAtReferredType()));
    }
}
