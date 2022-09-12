package org.fiware.tmforum.customer_bill.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

import java.net.URL;

@EqualsAndHashCode(callSuper = true)
@Data
public class PaymentMethodRef extends Entity {

    private String id;
    private URL href;
    private String name;
    private String atReferredType;

}
