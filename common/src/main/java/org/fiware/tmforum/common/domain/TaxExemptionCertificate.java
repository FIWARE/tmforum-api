package org.fiware.tmforum.common.domain;

import lombok.Data;

import java.util.List;

@Data
public class TaxExemptionCertificate {

	private String id;
	private AttachmentRefOrValue attachment;
	private List<TaxDefinition> taxDefinition;
	private TimePeriod validFor;
	private String atBaseType;
	private String atSchemaLocation;
	private String atType;
}
