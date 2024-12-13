package org.fiware.tmforum.customerbillmanagement.rest;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customerbillmanagement.api.ext.CustomerBillExtensionApi;
import org.fiware.customerbillmanagement.model.CustomerBillCreateVO;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customerbillmanagement.TMForumMapper;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
@Requires(property = "apiExtension.enabled", value = "true")
public class ExtendedCustomerBillApiController extends AbstractApiController<CustomerBill> implements CustomerBillExtensionApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	private final CustomerBillApiController customerBillApiController;

	public ExtendedCustomerBillApiController(QueryParser queryParser, ReferenceValidationService validationService, TmForumRepository repository, TMForumEventHandler eventHandler, TMForumMapper tmForumMapper, Clock clock, CustomerBillApiController customerBillApiController) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
		this.customerBillApiController = customerBillApiController;
	}

	@Override
	public Mono<HttpResponse<CustomerBillVO>> createCustomerBill(CustomerBillCreateVO customerBillCreateVO) {

		CustomerBill customerBill = tmForumMapper.map(
				tmForumMapper.map(customerBillCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), CustomerBill.TYPE_CUSTOMER_BILL)));
		customerBill.setLastUpdate(clock.instant());
		return create(customerBillApiController.getCheckingMono(customerBill), CustomerBill.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}


}
