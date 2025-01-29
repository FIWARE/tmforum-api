package org.fiware.tmforum.customerbillmanagement;

import org.fiware.customerbillmanagement.model.*;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.customerbillmanagement.domain.AppliedCustomerBillingRate;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBillOnDemand;
import io.github.wistefan.mapping.MappingException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public abstract class TMForumMapper extends BaseMapper {

	// customer bill

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract CustomerBillVO map(CustomerBillCreateVO customerBillCreateVO, URI id);

	public abstract CustomerBillVO map(CustomerBill customer);

	public abstract CustomerBill map(CustomerBillVO customerBillVOs);

	@Mapping(target = "id", source = "id")
	public abstract CustomerBill map(CustomerBillUpdateVO customerUpdateVO, String id);

	// customer bill on demand

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract CustomerBillOnDemandVO map(CustomerBillOnDemandCreateVO customerBillOnDemandCreateVO, URI id);

	public abstract CustomerBillOnDemandVO map(CustomerBillOnDemand customerBillOnDemand);

	public abstract CustomerBillOnDemand map(CustomerBillOnDemandVO customerBillOnDemandVO);

	// applied customer billing rate

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract AppliedCustomerBillingRateVO map(AppliedCustomerBillingRateCreateVO appliedCustomerBillingRateCreateVO, URI id);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract AppliedCustomerBillingRateVO map(AppliedCustomerBillingRateUpdateVO appliedCustomerBillingRateUpdateVO, String id);

	@Mapping(target = "rateType", source = "appliedBillingRateType")
	public abstract AppliedCustomerBillingRate map(AppliedCustomerBillingRateVO appliedCustomerBillingRateVO);

	@Mapping(target = "appliedBillingRateType", source = "rateType")
	public abstract AppliedCustomerBillingRateVO map(AppliedCustomerBillingRate appliedCustomerBillingRate);

	public URL map(String value) {
		if (value == null) {
			return null;
		}
		try {
			return new URL(value);
		} catch (MalformedURLException e) {
			throw new MappingException(String.format("%s is not a URL.", value), e);
		}
	}

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

	public String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}
}


