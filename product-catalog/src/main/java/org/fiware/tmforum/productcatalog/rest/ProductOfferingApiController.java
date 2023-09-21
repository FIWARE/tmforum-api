package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.ProductOfferingApi;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingUpdateVO;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.tmforum.common.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ProductOfferingApiController extends AbstractProductCatalogApiController<ProductOffering>
		implements ProductOfferingApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ProductOfferingApiController(ReferenceValidationService validationService,
			TmForumRepository productCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock, EventHandler eventHandler) {
		super(validationService, productCatalogRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ProductOfferingVO>> createProductOffering(
			ProductOfferingCreateVO productOfferingCreateVO) {
		ProductOffering productOffering = tmForumMapper.map(
				tmForumMapper.map(productOfferingCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), ProductOffering.TYPE_PRODUCT_OFFERING)));
		productOffering.setLastUpdate(clock.instant());

		return create(getCheckingMono(productOffering), ProductOffering.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<ProductOffering> getCheckingMono(ProductOffering productOffering) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(productOffering.getAgreement());
		references.add(productOffering.getCategory());
		references.add(productOffering.getChannel());
		references.add(productOffering.getMarketSegment());
		references.add(productOffering.getPlace());
		references.add(productOffering.getProductOfferingPrice());
		references.add(productOffering.getBundledProductOffering());
		references.add(productOffering.getProductOfferingRelationship());
		Optional.ofNullable(productOffering.getProductSpecification())
				.ifPresent(psRef -> references.add(List.of(psRef)));
		Optional.ofNullable(productOffering.getResourceCandidate()).ifPresent(rcRef -> references.add(List.of(rcRef)));
		Optional.ofNullable(productOffering.getServiceCandidate()).ifPresent(scRef -> references.add(List.of(scRef)));
		Optional.ofNullable(productOffering.getServiceLevelAgreement())
				.ifPresent(slaRef -> references.add(List.of(slaRef)));
		Optional.ofNullable(productOffering.getProdSpecCharValueUse())
				.ifPresent(pscvu -> references.add(validateProdSpecCharValueUse(pscvu)));

		Mono<ProductOffering> specCheckingMono = getCheckingMono(productOffering, references);

		return specCheckingMono
				.onErrorMap(throwable -> new TmForumException(
						String.format("Was not able to create product offering %s", productOffering.getId()), throwable,
						TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<Object>> deleteProductOffering(String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ProductOfferingVO>>> listProductOffering(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, ProductOffering.TYPE_PRODUCT_OFFERING, ProductOffering.class)
				.map(productOfferingStream -> productOfferingStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ProductOfferingVO>> patchProductOffering(String id,
			ProductOfferingUpdateVO productOffering) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such product offering cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		ProductOffering updatedProductOffering = tmForumMapper.map(tmForumMapper.map(productOffering, id));
		updatedProductOffering.setLastUpdate(clock.instant());

		return patch(id, updatedProductOffering, getCheckingMono(updatedProductOffering), ProductOffering.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ProductOfferingVO>> retrieveProductOffering(String id, @Nullable String fields) {
		return retrieve(id, ProductOffering.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such product offering exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
