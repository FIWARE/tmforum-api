package org.fiware.tmforum.productordering.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productordering.api.ProductOrderApi;
import org.fiware.productordering.model.ProductOrderCreateVO;
import org.fiware.productordering.model.ProductOrderUpdateVO;
import org.fiware.productordering.model.ProductOrderVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.product.PriceAlteration;
import org.fiware.tmforum.productordering.TMForumMapper;
import org.fiware.tmforum.productordering.domain.ProductOrder;
import org.fiware.tmforum.productordering.domain.ProductOrderItem;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller("${general.basepath:/}")
public class ProductOrderingApiController extends AbstractApiController<ProductOrder> implements ProductOrderApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ProductOrderingApiController(QueryParser queryParser, ReferenceValidationService validationService,
										TmForumRepository repository, TMForumMapper tmForumMapper, Clock clock, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ProductOrderVO>> createProductOrder(
			@NonNull ProductOrderCreateVO productOrderCreateVO) {
		ProductOrder productOrder = tmForumMapper.map(
				tmForumMapper.map(productOrderCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), ProductOrder.TYPE_PRODUCT_ORDER)));
		productOrder.setOrderDate(clock.instant());

		return create(getCheckingMono(productOrder), ProductOrder.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	@Override
	public Mono<HttpResponse<Object>> deleteProductOrder(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ProductOrderVO>>> listProductOrder(@Nullable String fields,
																	 @Nullable Integer offset,
																	 @Nullable Integer limit) {
		return list(offset, limit, ProductOrder.TYPE_PRODUCT_ORDER, ProductOrder.class)
				.map(productOrderStream -> productOrderStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ProductOrderVO>> patchProductOrder(@NonNull String id,
																@NonNull ProductOrderUpdateVO productOrderUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such product order cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		ProductOrder productOrder = tmForumMapper.map(productOrderUpdateVO, id);

		// the list is not allowed to be emptied
		if (Optional.ofNullable(productOrder.getProductOrderItem()).map(List::isEmpty).orElse(false)) {
			productOrder.setProductOrderItem(null);
		}

		return patch(id, productOrder, getCheckingMono(productOrder), ProductOrder.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ProductOrderVO>> retrieveProductOrder(@NonNull String id,
																   @Nullable String fields) {
		return retrieve(id, ProductOrder.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such product order exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	private Mono<ProductOrder> getCheckingMono(ProductOrder productOrder) {

		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(productOrder.getAgreement());
		Optional.ofNullable(productOrder.getBillingAccount()).map(List::of).ifPresent(references::add);
		references.add(productOrder.getChannel());
		references.add(productOrder.getPayment());
		references.add(productOrder.getProductOfferingQualification());
		references.add(productOrder.getQuote());
		references.add(productOrder.getRelatedParty());

		Optional.ofNullable(productOrder.getOrderTotalPrice()).ifPresent(otp -> otp.forEach(op -> {
			Optional.ofNullable(op.getBillingAccount()).map(List::of).ifPresent(references::add);
			Optional.ofNullable(op.getProductOfferingPrice()).map(List::of).ifPresent(references::add);
			Optional.ofNullable(op.getPriceAlteration())
					.ifPresent(pa -> references.add(pa.stream().map(PriceAlteration::getProductOfferingPrice).collect(
							Collectors.toList())));
		}));

		List<Mono<ProductOrder>> checkingMonos = Optional.ofNullable(productOrder.getProductOrderItem())
				.orElse(List.of())
				.stream()
				.map(poi -> getOrderItemCheckingMono(productOrder, poi))
				.collect(Collectors.toList());

		checkingMonos.add(getCheckingMono(productOrder, references));

		return Mono.zip(checkingMonos, po -> productOrder)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create product order %s", productOrder.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	private Mono<ProductOrder> getOrderItemCheckingMono(ProductOrder productOrder,
														ProductOrderItem productOrderItem) {

		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		Optional.ofNullable(productOrderItem.getAppointment()).map(List::of).ifPresent(references::add);
		Optional.ofNullable(productOrderItem.getBillingAccount()).map(List::of).ifPresent(references::add);

		Optional.ofNullable(productOrderItem.getProduct()).map((item) -> {
			return item.getEntityId();
		}).ifPresent((uri) -> {
			references.add(List.of(productOrderItem.getProduct()));
		});

		Optional.ofNullable(productOrderItem.getProductOffering()).map(List::of).ifPresent(references::add);

		// TODO: validate item rel, we just validate the ref now
		Optional.ofNullable(productOrderItem.getProductOfferingQualificationItem()).map(List::of)
				.ifPresent(references::add);
		Optional.ofNullable(productOrderItem.getQuoteItem()).map(List::of).ifPresent(references::add);

		references.add(productOrderItem.getPayment());
		references.add(productOrderItem.getQualification());

		if (productOrderItem.getItemPrice() != null && !productOrderItem.getItemPrice().isEmpty()) {
			productOrderItem.getItemPrice().forEach(ip -> {
				Optional.ofNullable(ip.getProductOfferingPrice()).map(List::of).ifPresent(references::add);
				Optional.ofNullable(ip.getBillingAccount()).map(List::of).ifPresent(references::add);
				Optional.ofNullable(ip.getPriceAlteration()).map(this::getReferencesFromPriceAlterations)
						.ifPresent(references::add);
			});
		}
		if (productOrderItem.getItemTotalPrice() != null && !productOrderItem.getItemTotalPrice().isEmpty()) {
			productOrderItem.getItemTotalPrice().forEach(itp -> {
				Optional.ofNullable(itp.getProductOfferingPrice()).map(List::of).ifPresent(references::add);
				Optional.ofNullable(itp.getBillingAccount()).map(List::of).ifPresent(references::add);
				Optional.ofNullable(itp.getPriceAlteration()).map(this::getReferencesFromPriceAlterations)
						.ifPresent(references::add);
			});
		}

		List<Mono<ProductOrder>> checkingMonos = new ArrayList<>();
		checkingMonos.add(getCheckingMono(productOrder, references));

		if (productOrderItem.getProductOrderItem() != null) {
			productOrderItem.getProductOrderItem().stream()
					.map(poi -> getOrderItemCheckingMono(productOrder, poi))
					.forEach(checkingMonos::add);
		}
		return Mono.zip(checkingMonos, poi -> productOrder);
	}

	private List<? extends ReferencedEntity> getReferencesFromPriceAlterations(List<PriceAlteration> priceAlterations) {
		return priceAlterations.stream()
				.map(PriceAlteration::getProductOfferingPrice)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}
