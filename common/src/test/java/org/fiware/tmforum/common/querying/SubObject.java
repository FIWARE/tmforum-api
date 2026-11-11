package org.fiware.tmforum.common.querying;

import lombok.Data;

@Data
public class SubObject {

	private String status;
	// name is reserved in NGSI-LD, persisted as tmfEscaped-valueType
	private String valueType;
}
