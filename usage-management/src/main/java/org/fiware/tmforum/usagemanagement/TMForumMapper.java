package org.fiware.tmforum.usagemanagement;

import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.usagemanagement.model.TimePeriodVO;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.fiware.usagemanagement.model.*;
import org.fiware.tmforum.usagemanagement.domain.Usage;
import org.fiware.tmforum.usagemanagement.domain.UsageSpecification;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;


@Mapper(componentModel = "jsr330", uses = { IdHelper.class, MappingHelper.class })
public interface TMForumMapper {

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	UsageVO map(UsageCreateVO usageCreateVO, URI id);

	@Mapping(target = "id", source = "id")
	Usage map(UsageUpdateVO usageUpdateVO, String id);

	@Mapping(target = "href", source = "id")
	Usage map(UsageVO usageVO);

	@Mapping(target = "id", source = "id")
	UsageVO map(Usage usage);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	UsageSpecificationVO map(UsageSpecificationCreateVO usageSpecificationCreate, URI id);

	@Mapping(target = "id", source = "id")
	UsageSpecification map(UsageSpecificationUpdateVO usageSpecificationUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	UsageSpecification map(UsageSpecificationVO usageSpecificationUpdateVO);

	@Mapping(target = "id", source = "id")
	UsageSpecificationVO map(UsageSpecification usageSpecificationUpdateVO);

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