package org.fiware.tmforum.party.domain;

import lombok.Data;

import java.time.Instant;

@Data
public class TimePeriod {

	private Instant endDateTime;
	private Instant startDateTime;
}
