package org.fiware.tmforum.productinventory.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productinventory.api.ProductApi;
import org.fiware.productinventory.model.ProductCreateVO;
import org.fiware.productinventory.model.ProductUpdateVO;
import org.fiware.productinventory.model.ProductVO;
import org.fiware.tmforum.common.domain.BillingAccountRef;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.product.PriceAlteration;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.product.ProductOfferingPriceRef;
import org.fiware.tmforum.product.ProductPrice;
import org.fiware.tmforum.product.ProductRelationship;
import org.fiware.tmforum.productinventory.TMForumMapper;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ProductApiController extends AbstractApiController<Product> implements ProductApi {

	private final TMForumMapper tmForumMapper;

	public ProductApiController(ReferenceValidationService validationService,
			TmForumRepository repository, TMForumMapper tmForumMapper) {
		super(validationService, repository);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<ProductVO>> createProduct(@NonNull ProductCreateVO productCreateVO) {
		Product product = tmForumMapper.map(
				tmForumMapper.map(productCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), Product.TYPE_PRODUCT)));

		return create(getCheckingMono(product), Product.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<Product> getCheckingMono(Product product) {

		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(product.getAgreement());
		references.add(product.getPlace());
		references.add(product.getProductOrderItem());
		references.add(product.getRealizingResource());
		references.add(product.getRealizingService());
		references.add(product.getRelatedParty());
		Optional.ofNullable(product.getBillingAccount()).map(List::of).ifPresent(references::add);
		Optional.ofNullable(product.getProductOffering()).map(List::of).ifPresent(references::add);
		Optional.ofNullable(product.getProductSpecification()).map(List::of).ifPresent(references::add);

		Mono<Product> checkingMono = getCheckingMono(product, references);

		if (product.getProductRelationship() != null && !product.getProductRelationship().isEmpty()) {
			List<Mono<Product>> productRelCheckingMonos = product.getProductRelationship()
					.stream()
					.map(ProductRelationship::getProduct)
					.map(productRef -> getCheckingMono(product, List.of(List.of(productRef))))
					.toList();
			if (!productRelCheckingMonos.isEmpty()) {
				Mono<Product> productRelCheckingMono = Mono.zip(productRelCheckingMonos, p -> product);
				checkingMono = Mono.zip(productRelCheckingMono, checkingMono, (p1, p2) -> product);
			}
		}

		if (product.getProductPrice() != null && !product.getProductPrice().isEmpty()) {
			List<List<? extends ReferencedEntity>> internalReferences = new ArrayList<>();

			List<BillingAccountRef> billingAccountRefs = product.getProductPrice()
					.stream()
					.map(ProductPrice::getBillingAccount)
					.filter(Objects::nonNull)
					.toList();
			internalReferences.add(billingAccountRefs);
			List<ProductOfferingPriceRef> productOfferingPriceRefs = product.getProductPrice()
					.stream()
					.map(ProductPrice::getProductOfferingPrice)
					.filter(Objects::nonNull)
					.toList();
			internalReferences.add(productOfferingPriceRefs);
			List<ProductOfferingPriceRef> alterationRefs = product.getProductPrice()
					.stream()
					.map(ProductPrice::getProductPriceAlteration)
					.filter(Objects::nonNull)
					.flatMap(List::stream)
					.map(PriceAlteration::getProductOfferingPrice)
					.filter(Objects::nonNull)
					.toList();
			internalReferences.add(alterationRefs);
			checkingMono = Mono.zip(getCheckingMono(product, internalReferences), checkingMono, (p1, p2) -> product);
		}
		return checkingMono
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create product %s", product.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override public Mono<HttpResponse<Object>> deleteProduct(@NonNull String id) {
		return delete(id);
	}

	@Override public Mono<HttpResponse<List<ProductVO>>> listProduct(@Nullable String fields, @Nullable Integer offset,
			@Nullable Integer limit) {
		return list(offset, limit, Product.TYPE_PRODUCT, Product.class)
				.map(productStream -> productStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override public Mono<HttpResponse<ProductVO>> patchProduct(@NonNull String id,
			@NonNull ProductUpdateVO productUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such product cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		Product product = tmForumMapper.map(productUpdateVO, id);

		return patch(id, product, getCheckingMono(product), Product.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override public Mono<HttpResponse<ProductVO>> retrieveProduct(@NonNull String id, @Nullable String fields) {
		return retrieve(id, Product.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such product exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
