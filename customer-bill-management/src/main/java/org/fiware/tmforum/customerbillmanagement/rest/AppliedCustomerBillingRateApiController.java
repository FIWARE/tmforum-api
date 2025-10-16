package org.fiware.tmforum.customerbillmanagement.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customerbillmanagement.api.AppliedCustomerBillingRateApi;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customerbillmanagement.TMForumMapper;
import org.fiware.tmforum.customerbillmanagement.domain.AppliedCustomerBillingRate;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Controller("${general.basepath:/}")
public class AppliedCustomerBillingRateApiController extends AbstractApiController<AppliedCustomerBillingRate>
		implements
		AppliedCustomerBillingRateApi {

	private final TMForumMapper tmForumMapper;

	public AppliedCustomerBillingRateApiController(
			QueryParser queryParser,
			ReferenceValidationService validationService,
			TmForumRepository repository, TMForumMapper tmForumMapper, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<List<AppliedCustomerBillingRateVO>>> listAppliedCustomerBillingRate(
			@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, AppliedCustomerBillingRate.TYPE_APPLIED_CUSTOMER_BILLING_RATE,
				AppliedCustomerBillingRate.class)
				.map(customerBillOnDemandStream -> customerBillOnDemandStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<AppliedCustomerBillingRateVO>> retrieveAppliedCustomerBillingRate(@NonNull String id,
			@Nullable String fields) {
		return retrieve(id, AppliedCustomerBillingRate.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such applied customer billing rate exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}


}
