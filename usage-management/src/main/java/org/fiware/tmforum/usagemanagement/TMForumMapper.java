package org.fiware.tmforum.usagemanagement;

import org.fiware.usagemanagement.model.TimePeriodVO;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.fiware.usagemanagement.model.UsageVO;
import org.fiware.usagemanagement.model.UsageCreateVO;
import org.fiware.usagemanagement.model.UsageUpdateVO;
import org.fiware.usagemanagement.model.UsageSpecificationVO;
import org.fiware.usagemanagement.model.UsageSpecificationCreateVO;
import org.fiware.usagemanagement.model.UsageSpecificationUpdateVO;
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

	@Mapping(target = "id", source = "id")
	Usage map(UsageVO usageUpdateVO);

	@Mapping(target = "id", source = "id")
	UsageVO map(Usage sageUpdateVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	UsageSpecificationVO map(UsageSpecificationCreateVO usageSpecificationCreate, URI id);

	@Mapping(target = "id", source = "id")
	UsageSpecification map(UsageSpecificationUpdateVO usageSpecificationUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	UsageSpecification map(UsageSpecificationVO usageSpecificationUpdateVO);

	@Mapping(target = "id", source = "id")
	UsageSpecificationVO map(UsageSpecification usageSpecificationUpdateVO);

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