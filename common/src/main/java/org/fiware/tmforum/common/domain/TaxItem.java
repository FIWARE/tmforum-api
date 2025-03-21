package org.fiware.tmforum.common.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Money;

import java.net.URI;

@Data
public class TaxItem {

	private String itemId;
	private URI href;
	private String taxCategory;
	private Float taxRate;
	private Money taxAmount;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
