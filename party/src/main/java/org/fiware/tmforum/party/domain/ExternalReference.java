package org.fiware.tmforum.party.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExternalReference extends Entity {

	private String externalReferenceType;
	private String name;
}
