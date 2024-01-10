package org.fiware.tmforum.customer;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.Ignore;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class PaymentMethodRef extends RefEntity {
    
    public PaymentMethodRef(URI id) {
        super(id);
    }

    @Override
    @Ignore
    public List<String> getReferencedTypes() {
        return List.of(getAtReferredType());
    }
}
