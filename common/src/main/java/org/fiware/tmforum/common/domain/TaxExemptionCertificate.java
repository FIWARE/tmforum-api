package org.fiware.tmforum.common.domain;

import lombok.Data;

import java.net.URI;
import java.util.List;

@Data
public class TaxExemptionCertificate {

	private String tmfId;
	private AttachmentRefOrValue attachment;
	private List<TaxDefinition> taxDefinition;
	private TimePeriod validFor;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
