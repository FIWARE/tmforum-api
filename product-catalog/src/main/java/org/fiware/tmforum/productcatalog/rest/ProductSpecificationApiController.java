package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.ProductSpecificationApi;
import org.fiware.productcatalog.model.ProductSpecificationCreateVO;
import org.fiware.productcatalog.model.ProductSpecificationUpdateVO;
import org.fiware.productcatalog.model.ProductSpecificationVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.product.ProductSpecification;
import org.fiware.tmforum.product.ProductSpecificationCharacteristic;
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
public class ProductSpecificationApiController extends AbstractProductCatalogApiController<ProductSpecification>
		implements ProductSpecificationApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ProductSpecificationApiController(ReferenceValidationService validationService,
			TmForumRepository productCatalogRepository, TMForumMapper tmForumMapper,
			Clock clock) {
		super(validationService, productCatalogRepository);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ProductSpecificationVO>> createProductSpecification(
			ProductSpecificationCreateVO productSpecificationCreateVO) {
		ProductSpecification productSpecification = tmForumMapper.map(
				tmForumMapper.map(productSpecificationCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(),
						ProductSpecification.TYPE_PRODUCT_SPECIFICATION)));
		productSpecification.setLastUpdate(clock.instant());

		return create(getCheckingMono(productSpecification), ProductSpecification.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);

	}

	private Mono<ProductSpecification> getCheckingMono(ProductSpecification productSpecification) {
		Optional.ofNullable(productSpecification.getProductSpecCharacteristic())
				.ifPresent(this::validateProductSpecificationCharacteristic);
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(productSpecification.getBundledProductSpecification());
		references.add(productSpecification.getProductSpecificationRelationship());
		references.add(productSpecification.getRelatedParty());
		references.add(productSpecification.getResourceSpecification());
		references.add(productSpecification.getServiceSpecification());

		return getCheckingMono(productSpecification, references)
				.onErrorMap(throwable -> new TmForumException(
						String.format("Was not able to create product specification %s", productSpecification.getId()),
						throwable, TmForumExceptionReason.INVALID_RELATIONSHIP));

	}

	private void validateProductSpecificationCharacteristic(
			List<ProductSpecificationCharacteristic> productSpecificationCharacteristics) {
		List<String> prodSpecCharIds = productSpecificationCharacteristics.stream()
				.map(ProductSpecificationCharacteristic::getId)
				.toList();
		if (prodSpecCharIds.size() != new HashSet<>(prodSpecCharIds).size()) {
			throw new TmForumException(
					String.format("Duplicate ids for product specification characteristics are not allowed - ids: %s",
							prodSpecCharIds), TmForumExceptionReason.INVALID_DATA);
		}
		productSpecificationCharacteristics.stream()
				.map(ProductSpecificationCharacteristic::getProductSpecCharRelationship)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.filter(refId -> !prodSpecCharIds.contains(refId))
				.findFirst()
				.ifPresent(refId -> {
					throw new TmForumException(
							String.format("Referenced product specification characteristics %s does not exist.", refId),
							TmForumExceptionReason.INVALID_DATA);
				});
	}

	@Override
	public Mono<HttpResponse<Object>> deleteProductSpecification(String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ProductSpecificationVO>>> listProductSpecification(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, ProductSpecification.TYPE_PRODUCT_SPECIFICATION, ProductSpecification.class)
				.map(productSpecificationStream -> productSpecificationStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ProductSpecificationVO>> patchProductSpecification(String id,
			ProductSpecificationUpdateVO productSpecification) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such product specification cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		ProductSpecification updatedProductSpecification = tmForumMapper.map(
				tmForumMapper.map(productSpecification, id));
		updatedProductSpecification.setLastUpdate(clock.instant());

		return patch(id, updatedProductSpecification, getCheckingMono(updatedProductSpecification),
				ProductSpecification.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ProductSpecificationVO>> retrieveProductSpecification(String id, @Nullable String fields) {
		return retrieve(id, ProductSpecification.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such product specification exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
