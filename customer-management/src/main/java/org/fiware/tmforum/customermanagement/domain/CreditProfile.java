package org.fiware.tmforum.customermanagement.domain;


import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.time.Instant;

@Data
public class CreditProfile extends Entity {

    private Instant creditProfileDate;
    private Instant creditRiskRating;
    private Integer creditScore;
    private TimePeriod validFor;
}
