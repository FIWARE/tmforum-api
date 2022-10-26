package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.ProductOfferingPriceApi;
import org.fiware.productcatalog.model.ProductOfferingPriceCreateVO;
import org.fiware.productcatalog.model.ProductOfferingPriceUpdateVO;
import org.fiware.productcatalog.model.ProductOfferingPriceVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import org.fiware.tmforum.productcatalog.domain.PricingLogicAlgorithm;
import org.fiware.tmforum.productcatalog.domain.ProductOfferingPrice;
import org.fiware.tmforum.productcatalog.domain.ProductSpecificationCharacteristicValueUse;
import org.fiware.tmforum.productcatalog.domain.TaxItem;
import org.fiware.tmforum.productcatalog.exception.ProductCatalogException;
import org.fiware.tmforum.productcatalog.exception.ProductCatalogExceptionReason;
import org.fiware.tmforum.productcatalog.repository.ProductCatalogRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ProductOfferingPriceApiController extends AbstractApiController implements ProductOfferingPriceApi {

    private final Clock clock;


    public ProductOfferingPriceApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ProductCatalogRepository productCatalogRepository, Clock clock) {
        super(tmForumMapper, validationService, productCatalogRepository);
        this.clock = clock;
    }

    @Override
    public Mono<HttpResponse<ProductOfferingPriceVO>> createProductOfferingPrice(ProductOfferingPriceCreateVO productOfferingPriceCreateVO) {
        ProductOfferingPrice productOfferingPrice = tmForumMapper.map(
                tmForumMapper.map(productOfferingPriceCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE)));
        productOfferingPrice.setLastUpdate(clock.instant());

        Mono<ProductOfferingPrice> checkingMono = getCheckingMono(productOfferingPrice);

        Mono<ProductOfferingPrice> priceLogicHandlingMono = relatedEntityHandlingMono(
                productOfferingPrice,
                checkingMono,
                productOfferingPrice.getPricingLogicAlgorithm(),
                productOfferingPrice::setPricingLogicAlgorithm,
                PricingLogicAlgorithm.class);

        Mono<ProductOfferingPrice> taxItemHandlingMono = relatedEntityHandlingMono(
                productOfferingPrice,
                priceLogicHandlingMono,
                productOfferingPrice.getTax(),
                productOfferingPrice::setTax,
                TaxItem.class);

        return create(taxItemHandlingMono, ProductOfferingPrice.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<ProductOfferingPrice> getCheckingMono(ProductOfferingPrice productOfferingPrice) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(productOfferingPrice.getBundledPopRelationship());
        references.add(productOfferingPrice.getConstraint());
        references.add(productOfferingPrice.getPlace());
        references.add(productOfferingPrice.getPopRelationship());

        Mono<ProductOfferingPrice> specCheckingMono = getCheckingMono(productOfferingPrice, references);

        if (productOfferingPrice.getProdSpecCharValueUse() != null && !productOfferingPrice.getProdSpecCharValueUse().isEmpty()) {
            List<Mono<ProductSpecificationCharacteristicValueUse>> checkingMonos = productOfferingPrice
                    .getProdSpecCharValueUse()
                    .stream()
                    .filter(pscv -> pscv.getProductSpecification() != null)
                    .map(pcsv -> getCheckingMono(pcsv, List.of(List.of(pcsv.getProductSpecification()))))
                    .toList();
            Mono<ProductOfferingPrice> pscvCheckingMono = Mono.zip(checkingMonos, (m1) -> productOfferingPrice);
            specCheckingMono = Mono.zip(specCheckingMono, pscvCheckingMono, (p1, p2) -> productOfferingPrice);
        }

        return specCheckingMono
                .onErrorMap(throwable -> new ProductCatalogException(String.format("Was not able to create product offering price %s", productOfferingPrice.getId()), throwable, ProductCatalogExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteProductOfferingPrice(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<ProductOfferingPriceVO>>> listProductOfferingPrice(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE, ProductOfferingPrice.class)
                .map(productOfferingPriceStream -> productOfferingPriceStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ProductOfferingPriceVO>> patchProductOfferingPrice(String id, ProductOfferingPriceUpdateVO productOfferingPrice) {

        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new ProductCatalogException("Did not receive a valid id, such product offering cannot exist.", ProductCatalogExceptionReason.NOT_FOUND);
        }
        ProductOfferingPrice updatedProductOfferingPrice = tmForumMapper.map(tmForumMapper.map(productOfferingPrice, id));
        updatedProductOfferingPrice.setLastUpdate(clock.instant());
        Mono<ProductOfferingPrice> checkingMono = getCheckingMono(updatedProductOfferingPrice);

        Mono<ProductOfferingPrice> priceLogicHandlingMono = relatedEntityHandlingMono(
                updatedProductOfferingPrice,
                checkingMono,
                updatedProductOfferingPrice.getPricingLogicAlgorithm(),
                updatedProductOfferingPrice::setPricingLogicAlgorithm,
                PricingLogicAlgorithm.class);

        Mono<ProductOfferingPrice> taxItemHandlingMono = relatedEntityHandlingMono(
                updatedProductOfferingPrice,
                priceLogicHandlingMono,
                updatedProductOfferingPrice.getTax(),
                updatedProductOfferingPrice::setTax,
                TaxItem.class);

        return patch(id, updatedProductOfferingPrice, taxItemHandlingMono, ProductOfferingPrice.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ProductOfferingPriceVO>> retrieveProductOfferingPrice(String id, @Nullable String fields) {
        return retrieve(id, ProductOfferingPrice.class)
                .switchIfEmpty(Mono.error(new ProductCatalogException("No such product offering price exists.", ProductCatalogExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
