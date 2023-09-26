package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.ProductOfferingPriceApi;
import org.fiware.productcatalog.model.ProductOfferingPriceCreateVO;
import org.fiware.productcatalog.model.ProductOfferingPriceUpdateVO;
import org.fiware.productcatalog.model.ProductOfferingPriceVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.domain.TaxItem;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.product.PricingLogicAlgorithm;
import org.fiware.tmforum.product.ProductOfferingPrice;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ProductOfferingPriceApiController extends AbstractProductCatalogApiController<ProductOfferingPrice>
		implements ProductOfferingPriceApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ProductOfferingPriceApiController(ReferenceValidationService validationService,
			TmForumRepository productCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock, EventHandler eventHandler) {
		super(validationService, productCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ProductOfferingPriceVO>> createProductOfferingPrice(
			ProductOfferingPriceCreateVO productOfferingPriceCreateVO) {
		ProductOfferingPrice productOfferingPrice = tmForumMapper.map(
				tmForumMapper.map(productOfferingPriceCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(),
						ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE)));
		productOfferingPrice.setLastUpdate(clock.instant());

		return create(getCheckingMono(productOfferingPrice), ProductOfferingPrice.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<ProductOfferingPrice> getCheckingMono(ProductOfferingPrice productOfferingPrice) {
		Optional.ofNullable(productOfferingPrice.getPricingLogicAlgorithm())
				.ifPresent(this::validatePricingLogicAlgorithm);
		Optional.ofNullable(productOfferingPrice.getTax())
				.ifPresent(this::validateTaxItems);

		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		Optional.ofNullable(productOfferingPrice.getProdSpecCharValueUse())
				.ifPresent(pscvu -> references.add(validateProdSpecCharValueUse(pscvu)));
		references.add(productOfferingPrice.getBundledPopRelationship());
		references.add(productOfferingPrice.getConstraint());
		references.add(productOfferingPrice.getPlace());
		references.add(productOfferingPrice.getPopRelationship());

		return getCheckingMono(productOfferingPrice, references)
				.onErrorMap(throwable -> new TmForumException(
						String.format("Was not able to create product offering price %s", productOfferingPrice.getId()),
						throwable, TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	private void validatePricingLogicAlgorithm(List<PricingLogicAlgorithm> pricingLogicAlgorithms) {
		List<String> plaIds = pricingLogicAlgorithms.stream()
				.map(PricingLogicAlgorithm::getId)
				.toList();
		if (plaIds.size() != new HashSet<>(plaIds).size()) {
			throw new TmForumException(
					String.format("Duplicate ids for pricing logic algorithms are not allowed - ids: %s", plaIds),
					TmForumExceptionReason.INVALID_DATA);
		}
		pricingLogicAlgorithms.stream()
				.map(PricingLogicAlgorithm::getPlaSpecId)
				.filter(Objects::nonNull)
				.filter(refId -> !plaIds.contains(refId))
				.findFirst()
				.ifPresent(id -> {
					throw new TmForumException(
							String.format("Referenced pricing logic algorithm %s does not exist.", id),
							TmForumExceptionReason.INVALID_DATA);
				});
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

	@Override
	public Mono<HttpResponse<Object>> deleteProductOfferingPrice(String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ProductOfferingPriceVO>>> listProductOfferingPrice(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE, ProductOfferingPrice.class)
				.map(productOfferingPriceStream -> productOfferingPriceStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ProductOfferingPriceVO>> patchProductOfferingPrice(String id,
			ProductOfferingPriceUpdateVO productOfferingPrice) {

		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such product offering price cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		ProductOfferingPrice updatedProductOfferingPrice = tmForumMapper.map(
				tmForumMapper.map(productOfferingPrice, id));
		updatedProductOfferingPrice.setLastUpdate(clock.instant());

		return patch(id, updatedProductOfferingPrice, getCheckingMono(updatedProductOfferingPrice),
				ProductOfferingPrice.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ProductOfferingPriceVO>> retrieveProductOfferingPrice(String id, @Nullable String fields) {
		return retrieve(id, ProductOfferingPrice.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such product offering price exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
