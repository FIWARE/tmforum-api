package org.fiware.tmforum.customer.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreditProfile extends Entity {

    private Instant creditProfileDate;
    private Integer creditRiskRating;
    private Integer creditScore;

}
