package org.fiware.tmforum.common.domain;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediumCharacteristic extends Entity {

	private String city;
	private String contactType;
	private String country;
	private String emailAddress;
	private String faxNumber;
	private String phoneNumber;
	private String postCode;
	private String socialNetworkId;
	private String stateOrProvince;
	private String street1;
	private String street2;
}
