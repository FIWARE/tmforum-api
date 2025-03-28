package org.fiware.tmforum.quote.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.RelatedParty;

import java.time.Instant;
import java.util.List;

@Data
public class Authorization extends Entity {

	private Instant givenDate;
	private String name;
	private Instant requestedDate;
	private String signatureRepresentation;
	private String state;
	private List<RelatedParty> approver;
}
