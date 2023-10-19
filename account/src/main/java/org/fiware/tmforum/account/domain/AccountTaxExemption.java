package org.fiware.tmforum.account.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
@EqualsAndHashCode(callSuper = true)
public class AccountTaxExemption extends Entity {

    private String certificateNumber;
    private String issuingJurisdiction;
    private String reason;
    private TimePeriod validFor;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
