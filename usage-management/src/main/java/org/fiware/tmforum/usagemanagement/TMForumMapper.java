package org.fiware.tmforum.usagemanagement;

import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.product.Characteristic;
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


@Mapper(componentModel = "jsr330", uses = {IdHelper.class, MappingHelper.class})
public abstract class TMForumMapper extends BaseMapper {

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract UsageVO map(UsageCreateVO usageCreateVO, URI id);

	@Mapping(target = "id", source = "id")
	public abstract Usage map(UsageUpdateVO usageUpdateVO, String id);

	@Mapping(target = "href", source = "id")
	public abstract Usage map(UsageVO usageVO);

	@Mapping(target = "id", source = "id")
	public abstract UsageVO map(Usage usage);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract UsageSpecificationVO map(UsageSpecificationCreateVO usageSpecificationCreate, URI id);

	@Mapping(target = "id", source = "id")
	public abstract UsageSpecification map(UsageSpecificationUpdateVO usageSpecificationUpdateVO, String id);

	@Mapping(target = "id", source = "id")
	public abstract UsageSpecification map(UsageSpecificationVO usageSpecificationUpdateVO);

	@Mapping(target = "id", source = "id")
	public abstract UsageSpecificationVO map(UsageSpecification usageSpecificationUpdateVO);

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	@Mapping(target = "charValue", source = "value")
	public abstract Characteristic map(CharacteristicVO characteristicVO);

	@Mapping(target = "value", source = "charValue")
	public abstract CharacteristicVO map(Characteristic characteristic);

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