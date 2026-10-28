package org.fiware.tmforum.customer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.mapping.Default;
import io.github.wistefan.mapping.annotations.Ignore;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class PaymentMethodRef extends RefEntity {

    @Default
    public PaymentMethodRef(@JsonProperty("id") URI id) {
        super(id);
    }

    public PaymentMethodRef(String id) {
        this(URI.create(id));
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(getAtReferredType()));
    }
}
