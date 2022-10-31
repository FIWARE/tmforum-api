package org.fiware.tmforum.customerbillmanagement.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customerbillmanagement.api.CustomerBillApi;
import org.fiware.customerbillmanagement.model.CustomerBillUpdateVO;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customerbillmanagement.TMForumMapper;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Controller("${general.basepath:/}")
public class CustomerBillApiController extends AbstractApiController<CustomerBill> implements CustomerBillApi {

	private final TMForumMapper tmForumMapper;

	public CustomerBillApiController(ReferenceValidationService validationService,
			TmForumRepository repository, TMForumMapper tmForumMapper) {
		super(validationService, repository);
		this.tmForumMapper = tmForumMapper;
	}

	@Override public Mono<HttpResponse<List<CustomerBillVO>>> listCustomerBill(String fields, Integer offset,
			Integer limit) {
		return null;
	}

	@Override public Mono<HttpResponse<CustomerBillVO>> patchCustomerBill(String id,
			CustomerBillUpdateVO customerBill) {
		return null;
	}

	@Override public Mono<HttpResponse<CustomerBillVO>> retrieveCustomerBill(String id, String fields) {
		return null;
	}
}
