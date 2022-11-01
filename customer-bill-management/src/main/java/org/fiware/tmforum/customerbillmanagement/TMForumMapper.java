package org.fiware.tmforum.customerbillmanagement;

import org.fiware.customerbillmanagement.model.CustomerBillOnDemandCreateVO;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandVO;
import org.fiware.customerbillmanagement.model.CustomerBillUpdateVO;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBillOnDemand;
import org.fiware.tmforum.mapping.MappingException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public interface TMForumMapper {

	// customer bill

	CustomerBillVO map(CustomerBill customer);

	CustomerBill map(CustomerBillVO customerBillVOs);

	@Mapping(target = "id", source = "id")
	CustomerBill map(CustomerBillUpdateVO customerUpdateVO, String id);

	// customer bill on demand

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	CustomerBillOnDemandVO map(CustomerBillOnDemandCreateVO customerBillOnDemandCreateVO, URI id);

	CustomerBillOnDemandVO map(CustomerBillOnDemand customerBillOnDemand);

	CustomerBillOnDemand map(CustomerBillOnDemandVO customerBillOnDemandVO);

	default URL map(String value) {
		if (value == null) {
			return null;
		}
		try {
			return new URL(value);
		} catch (MalformedURLException e) {
			throw new MappingException(String.format("%s is not a URL.", value), e);
		}
	}

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

	default String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}
}


