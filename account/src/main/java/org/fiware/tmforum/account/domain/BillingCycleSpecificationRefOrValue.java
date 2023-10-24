package org.fiware.tmforum.account.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.domain.Entity;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BillingCycleSpecificationRefOrValue extends Entity {

    private URI id;
    private URI href;
    private Integer billingDateShift;
    private String billingPeriod;
    private Integer chargeDateOffset;
    private Integer creditDateOffset;
    private Integer dateShift;
    private String description;
    private String frequency;
    private Boolean isRef;
    private Integer mailingDateOffset;
    private String name;
    private Integer paymentDueDateOffset;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atReferredType;

}
