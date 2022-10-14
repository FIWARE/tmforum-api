package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.ProductOfferingApi;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingUpdateVO;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import org.fiware.tmforum.productcatalog.domain.BundleProductOffering;
import org.fiware.tmforum.productcatalog.domain.ProductOffering;
import org.fiware.tmforum.productcatalog.domain.ProductSpecification;
import org.fiware.tmforum.productcatalog.domain.ProductSpecificationCharacteristic;
import org.fiware.tmforum.productcatalog.domain.ProductSpecificationCharacteristicValueUse;
import org.fiware.tmforum.productcatalog.exception.ProductCatalogException;
import org.fiware.tmforum.productcatalog.exception.ProductCatalogExceptionReason;
import org.fiware.tmforum.productcatalog.repository.ProductCatalogRepository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class ProductOfferingApiController extends AbstractApiController implements ProductOfferingApi {

    private final Clock clock;

    public ProductOfferingApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ProductCatalogRepository productCatalogRepository, Clock clock) {
        super(tmForumMapper, validationService, productCatalogRepository);
        this.clock = clock;
    }

    @Override
    public Mono<HttpResponse<ProductOfferingVO>> createProductOffering(ProductOfferingCreateVO productOfferingCreateVO) {
        ProductOffering productOffering = tmForumMapper.map(
                tmForumMapper.map(productOfferingCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), ProductOffering.TYPE_PRODUCT_OFFERING)));
        productOffering.setLastUpdate(clock.instant());

        Mono<ProductOffering> checkingMono = getCheckingMono(productOffering);

        Mono<ProductOffering> productSpecCharHandlingMono = relatedEntityHandlingMono(
                productOffering,
                checkingMono,
                productOffering.getProdSpecCharValueUse(),
                productOffering::setProdSpecCharValueUse,
                ProductSpecificationCharacteristicValueUse.class);

        return create(productSpecCharHandlingMono, ProductOffering.class)
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
        Optional.ofNullable(productOffering.getProductSpecification()).ifPresent(psRef -> references.add(List.of(psRef)));
        Optional.ofNullable(productOffering.getResourceCandidate()).ifPresent(rcRef -> references.add(List.of(rcRef)));
        Optional.ofNullable(productOffering.getServiceCandidate()).ifPresent(scRef -> references.add(List.of(scRef)));
        Optional.ofNullable(productOffering.getServiceLevelAgreement()).ifPresent(slaRef -> references.add(List.of(slaRef)));

        Mono<ProductOffering> specCheckingMono = getCheckingMono(productOffering, references);

        if (productOffering.getProdSpecCharValueUse() != null && !productOffering.getProdSpecCharValueUse().isEmpty()) {
            List<Mono<ProductSpecificationCharacteristicValueUse>> checkingMonos = productOffering
                    .getProdSpecCharValueUse()
                    .stream()
                    .filter(pscv -> pscv.getProductSpecification() != null)
                    .map(pcsv -> getCheckingMono(pcsv, List.of(List.of(pcsv.getProductSpecification()))))
                    .toList();
            Mono<ProductOffering> pscvCheckingMono = Mono.zip(checkingMonos, (m1) -> productOffering);
            specCheckingMono = Mono.zip(specCheckingMono, pscvCheckingMono, (p1, p2) -> productOffering);
        }

        return specCheckingMono
                .onErrorMap(throwable -> new ProductCatalogException(String.format("Was not able to create product offering %s", productOffering.getId()), throwable, ProductCatalogExceptionReason.INVALID_RELATIONSHIP));
    }


    @Override
    public Mono<HttpResponse<Object>> deleteProductOffering(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<ProductOfferingVO>>> listProductOffering(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, ProductOffering.TYPE_PRODUCT_OFFERING, ProductOffering.class)
                .map(productOfferingStream -> productOfferingStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ProductOfferingVO>> patchProductOffering(String id, ProductOfferingUpdateVO productOffering) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new ProductCatalogException("Did not receive a valid id, such product offering cannot exist.", ProductCatalogExceptionReason.NOT_FOUND);
        }
        ProductOffering updatedProductOffering = tmForumMapper.map(tmForumMapper.map(productOffering, id));
        updatedProductOffering.setLastUpdate(clock.instant());
        Mono<ProductOffering> checkingMono = getCheckingMono(updatedProductOffering);

        Mono<ProductOffering> productSpecCharHandlingMono = relatedEntityHandlingMono(
                updatedProductOffering,
                checkingMono,
                updatedProductOffering.getProdSpecCharValueUse(),
                updatedProductOffering::setProdSpecCharValueUse,
                ProductSpecificationCharacteristicValueUse.class);

        return patch(id, updatedProductOffering, productSpecCharHandlingMono, ProductOffering.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ProductOfferingVO>> retrieveProductOffering(String id, @Nullable String fields) {
        return retrieve(id, ProductOffering.class)
                .switchIfEmpty(Mono.error(new ProductCatalogException("No such product offering exists.", ProductCatalogExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
