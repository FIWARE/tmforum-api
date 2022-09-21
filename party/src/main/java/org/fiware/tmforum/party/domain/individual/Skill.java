package org.fiware.tmforum.party.domain.individual;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.party.domain.TimePeriod;

@Data
@EqualsAndHashCode(callSuper = true)
public class Skill extends Entity {

	private String comment;
	private String evaluatedLevel;
	private String skillCode;
	private String skillName;
	private TimePeriod validFor;
}
