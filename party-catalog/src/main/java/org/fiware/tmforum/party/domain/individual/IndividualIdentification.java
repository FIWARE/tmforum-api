package org.fiware.tmforum.party.domain.individual;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
public class IndividualIdentification extends Entity {

	private String identificationId;
	private String identificationType;
	private String issuingAuthority;
	private Instant issuingDate;
	private AttachmentRefOrValue attachment;
	private TimePeriod validFor;
}
