package org.fiware.tmforum.customerbillmanagement.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customerbillmanagement.api.CustomerBillOnDemandApi;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandCreateVO;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.customerbillmanagement.TMForumMapper;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBillOnDemand;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class CustomerBillOnDemandApiController extends AbstractApiController<CustomerBillOnDemand>
		implements CustomerBillOnDemandApi {

	private final TMForumMapper tmForumMapper;

	public CustomerBillOnDemandApiController(
			QueryParser queryParser,
			ReferenceValidationService validationService,
			TmForumRepository repository, TMForumMapper tmForumMapper, EventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<CustomerBillOnDemandVO>> createCustomerBillOnDemand(
			@NonNull CustomerBillOnDemandCreateVO customerBillOnDemandCreateVO) {
		CustomerBillOnDemand customerBillOnDemand = tmForumMapper.map(
				tmForumMapper.map(customerBillOnDemandCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(),
								CustomerBillOnDemand.TYPE_CUSTOMER_BILL_ON_DEMAND)));

		return create(getCheckingMono(customerBillOnDemand), CustomerBillOnDemand.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<CustomerBillOnDemand> getCheckingMono(CustomerBillOnDemand customerBillOnDemand) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		Optional.ofNullable(customerBillOnDemand.getBillingAccount()).map(List::of).ifPresent(references::add);
		Optional.ofNullable(customerBillOnDemand.getRelatedParty()).map(List::of).ifPresent(references::add);
		Optional.ofNullable(customerBillOnDemand.getCustomerBill()).map(List::of).ifPresent(references::add);

		return getCheckingMono(customerBillOnDemand, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create customer bill on demand %s",
										customerBillOnDemand.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<List<CustomerBillOnDemandVO>>> listCustomerBillOnDemand(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, CustomerBillOnDemand.TYPE_CUSTOMER_BILL_ON_DEMAND, CustomerBillOnDemand.class)
				.map(customerBillOnDemandStream -> customerBillOnDemandStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<CustomerBillOnDemandVO>> retrieveCustomerBillOnDemand(@NonNull String id,
			@Nullable String fields) {
		return retrieve(id, CustomerBillOnDemand.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such customer bill on demand exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
