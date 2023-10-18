package org.fiware.tmforum.account.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import java.util.List;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
@EqualsAndHashCode(callSuper = true)
public class BillStructure extends Entity {

    private BillingCycleSpecificationRefOrValue cycleSpecification;
    private BillFormatRefOrValue format;
    private List<BillPresentationMediaRefOrValue> presentationMedia;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}