package org.fiware.tmforum.servicecatalog.domain;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class AssociationSpecificationRef extends RefEntity {

    public AssociationSpecificationRef(URI id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        // maybe there is a concrete type in the future
        return List.of(getAtReferredType());
    }
}
