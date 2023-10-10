package org.fiware.tmforum.account.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class BillStructure {

    private BillingCycleSpecificationRefOrValue cycleSpecification;
    private BillFormatRefOrValue format;
    private BillPresentationMediaRefOrValue presentationMediaRefOrValue;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}