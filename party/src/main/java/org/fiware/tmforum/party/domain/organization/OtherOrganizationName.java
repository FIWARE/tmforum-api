package org.fiware.tmforum.party.domain.organization;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.party.domain.TimePeriod;

@Data
@EqualsAndHashCode(callSuper = true)
public class OtherOrganizationName extends Entity {

	private String name;
	private String nameType;
	private String tradingName;
	private TimePeriod validFor;
}
