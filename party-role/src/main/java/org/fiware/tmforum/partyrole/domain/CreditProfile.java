package org.fiware.tmforum.partyrole.domain;

import java.time.Instant;

import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.TimePeriod;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreditProfile extends Entity{
    private Instant creditProfileDate;
    private int creditRiskRating;
    private int creditScore;
    private TimePeriod validFor;
}
