package org.fiware.tmforum.party.domain.individual;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.party.domain.TimePeriod;

@Data
@EqualsAndHashCode(callSuper = true)
public class Disability extends Entity {

	private String disabilityCode;
	private String disabilityName;
	private TimePeriod validFor;
}
