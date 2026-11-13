package org.fiware.tmforum.common.querying;

import lombok.Data;

import java.time.Instant;

@Data
public class SubObject {

	private String status;
	// name is reserved in NGSI-LD, persisted as tmfEscaped-valueType
	private String valueType;
	// a date-time below a plain (non-@MappingEnabled) class, like TimePeriod.startDateTime
	private Instant since;
}
