package org.fiware.tmforum.agreement.domain;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgreementAuth extends Entity{
    private Instant date;
    private String signatureRepresentation;
    private String state;

}
