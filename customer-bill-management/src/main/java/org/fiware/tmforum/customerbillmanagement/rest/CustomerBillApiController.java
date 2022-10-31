package org.fiware.tmforum.customerbillmanagement.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customerbillmanagement.api.CustomerBillApi;
import org.fiware.customerbillmanagement.model.CustomerBillUpdateVO;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.tmforum.common.domain.TaxItem;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.customerbillmanagement.TMForumMapper;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller("${general.basepath:/}")
public class CustomerBillApiController extends AbstractApiController<CustomerBill> implements CustomerBillApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public CustomerBillApiController(ReferenceValidationService validationService,
			TmForumRepository repository, TMForumMapper tmForumMapper, Clock clock) {
		super(validationService, repository);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override public Mono<HttpResponse<List<CustomerBillVO>>> listCustomerBill(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, CustomerBill.TYPE_CUSTOMER_BILL, CustomerBill.class)
				.map(customerStream -> customerStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	private Mono<CustomerBill> getCheckingMono(CustomerBill customer) {
		Optional.ofNullable(customer.getTaxItem()).ifPresent(this::validateTaxItems);

		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		Optional.ofNullable(customer.getBillingAccount()).map(List::of).ifPresent(references::add);
		Optional.ofNullable(customer.getFinancialAccount()).map(List::of).ifPresent(references::add);
		Optional.ofNullable(customer.getPaymentMethod()).map(List::of).ifPresent(references::add);
		references.add(customer.getRelatedParty());

		return getCheckingMono(customer, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create customer %s", customer.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	private void validateTaxItems(List<TaxItem> taxItems) {
		List<String> taxItemIds = taxItems.stream()
				.map(TaxItem::getId)
				.toList();
		if (taxItemIds.size() != new HashSet<>(taxItemIds).size()) {
			throw new TmForumException(String.format("Duplicate taxItem ids are not allowed - ids: %s", taxItemIds),
					TmForumExceptionReason.INVALID_DATA);
		}
	}

	@Override public Mono<HttpResponse<CustomerBillVO>> patchCustomerBill(@NonNull String id,
			@NonNull CustomerBillUpdateVO customerBillUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such customer cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		CustomerBill customerBill = tmForumMapper.map(customerBillUpdateVO, id);
		customerBill.setLastUpdate(clock.instant());

		return patch(id, customerBill, getCheckingMono(customerBill), CustomerBill.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override public Mono<HttpResponse<CustomerBillVO>> retrieveCustomerBill(@NonNull String id, @Nullable String
			fields) {
		return retrieve(id, CustomerBill.class)
				.switchIfEmpty(
						Mono.error(new TmForumException("No such customer bill exists.",
								TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
