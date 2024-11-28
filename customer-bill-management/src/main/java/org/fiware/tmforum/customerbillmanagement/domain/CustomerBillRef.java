package org.fiware.tmforum.customerbillmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class CustomerBillRef extends RefEntity {

    public CustomerBillRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(CustomerBill.TYPE_CUSTOMER_BILL));
    }
}
