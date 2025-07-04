package org.fiware.tmforum.agreement;

import org.fiware.agreement.model.*;
import org.fiware.tmforum.agreement.domain.Agreement;
import org.fiware.tmforum.agreement.domain.AgreementSpecCharacteristicValue;
import org.fiware.tmforum.agreement.domain.AgreementSpecification;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.product.Characteristic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@Mapper(componentModel = "jsr330", uses = {IdHelper.class, MappingHelper.class})
public abstract class TMForumMapper extends BaseMapper {

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract AgreementVO map(AgreementCreateVO agreementCreateVO, URI id);

	@Mapping(target = "id", source = "id")
	public abstract Agreement map(AgreementUpdateVO agreementUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	public abstract Agreement map(AgreementVO agreementUpdateVO);

	@Mapping(target = "id", source = "id")
	public abstract AgreementVO map(Agreement agreementUpdateVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract AgreementSpecificationVO map(AgreementSpecificationCreateVO agreementspecCreate, URI id);

	@Mapping(target = "id", source = "id")
	public abstract AgreementSpecification map(AgreementSpecificationUpdateVO agreementspecUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	public abstract AgreementSpecification map(AgreementSpecificationVO agreementspecUpdateVO);

	@Mapping(target = "id", source = "id")
	public abstract AgreementSpecificationVO map(AgreementSpecification agreementspecUpdateVO);

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	@Mapping(target = "charValue", source = "value")
	public abstract Characteristic map(CharacteristicVO characteristicVO);

	@Mapping(target = "value", source = "charValue")
	public abstract CharacteristicVO map(Characteristic characteristic);

	@Mapping(target = "charValue", source = "value")
	public abstract AgreementSpecCharacteristicValue map(AgreementSpecCharacteristicValueVO characteristicVO);

	@Mapping(target = "value", source = "charValue")
	public abstract AgreementSpecCharacteristicValueVO map(AgreementSpecCharacteristicValue characteristic);

	public abstract TimePeriod map(TimePeriodVO value);

	public String map(URL value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	public URI mapToURI(String value) {
		if (value == null) {
			return null;
		}
		return URI.create(value);
	}

	public URL mapToURL(String value) {
		if (value == null) {
			return null;
		}
		try {
			return new URL(value);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

}
