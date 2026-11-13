package org.fiware.tmforum.common.querying;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class SubObject {

	private String status;
	// name is reserved in NGSI-LD, persisted as tmfEscaped-valueType
	private String valueType;
	// a date-time below a plain (non-@MappingEnabled) class, like TimePeriod.startDateTime
	private Instant since;
	// date-time collections of a plain class
	private List<Instant> history;
	private Instant[] milestones;
}
