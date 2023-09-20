package org.fiware.tmforum.agreement;

import org.fiware.agreement.model.*;
import org.fiware.tmforum.agreement.domain.Agreement;
import org.fiware.tmforum.agreement.domain.AgreementSpecification;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@Mapper(componentModel = "jsr330", uses = { IdHelper.class, MappingHelper.class })
public interface TMForumMapper {

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	AgreementVO map(AgreementCreateVO agreementCreateVO, URI id);

	@Mapping(target = "id", source = "id")
	Agreement map(AgreementUpdateVO agreementUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	Agreement map(AgreementVO agreementUpdateVO);

	@Mapping(target = "id", source = "id")
	AgreementVO map(Agreement agreementUpdateVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	AgreementSpecificationVO map(AgreementSpecificationCreateVO agreementspecCreate, URI id);

	@Mapping(target = "id", source = "id")
	AgreementSpecification map(AgreementSpecificationUpdateVO agreementspecUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	AgreementSpecification map(AgreementSpecificationVO agreementspecUpdateVO);

	@Mapping(target = "id", source = "id")
	AgreementSpecificationVO map(AgreementSpecification agreementspecUpdateVO);

	@Mapping(target = "query", source = "rawQuery")
	EventSubscriptionVO map(Subscription subscription);

	TimePeriod map(TimePeriodVO value);

	default String map(URL value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	default URI mapToURI(String value) {
		if (value == null) {
			return null;
		}
		return URI.create(value);
	}

	default URL mapToURL(String value) {
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

	default String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

}
