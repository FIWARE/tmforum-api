package org.fiware.tmforum.productcatalog.rest;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productcatalog.api.ProductSpecificationApi;
import org.fiware.productcatalog.model.ProductSpecificationCreateVO;
import org.fiware.productcatalog.model.ProductSpecificationUpdateVO;
import org.fiware.productcatalog.model.ProductSpecificationVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.productcatalog.TMForumMapper;
import org.fiware.tmforum.productcatalog.domain.ProductSpecification;
import org.fiware.tmforum.productcatalog.domain.ProductSpecificationCharacteristic;
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
public class ProductSpecificationApiController extends AbstractApiController implements ProductSpecificationApi {

    private final Clock clock;

    public ProductSpecificationApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ProductCatalogRepository productCatalogRepository, Clock clock) {
        super(tmForumMapper, validationService, productCatalogRepository);
        this.clock = clock;
    }

    @Override
    public Mono<HttpResponse<ProductSpecificationVO>> createProductSpecification(ProductSpecificationCreateVO productSpecificationCreateVO) {
        ProductSpecification productSpecification = tmForumMapper.map(
                tmForumMapper.map(productSpecificationCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), ProductSpecification.TYPE_PRODUCT_SPECIFICATION)));
        productSpecification.setLastUpdate(clock.instant());

        Mono<ProductSpecification> checkingMono = getCheckingMono(productSpecification);

        Mono<ProductSpecification> productSpecCharHandlingMono = relatedEntityHandlingMono(
                productSpecification,
                checkingMono,
                productSpecification.getProductSpecCharacteristic(),
                productSpecification::setProductSpecCharacteristic,
                ProductSpecificationCharacteristic.class);

        return create(productSpecCharHandlingMono, ProductSpecification.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);

    }

    private Mono<ProductSpecification> getCheckingMono(ProductSpecification productSpecification) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(productSpecification.getBundledProductSpecification());
        references.add(productSpecification.getProductSpecificationRelationship());
        references.add(productSpecification.getRelatedParty());
        references.add(productSpecification.getResourceSpecification());
        references.add(productSpecification.getServiceSpecification());

        Mono<ProductSpecification> specCheckingMono = getCheckingMono(productSpecification, references);

        if (productSpecification.getProductSpecCharacteristic() != null && !productSpecification.getProductSpecCharacteristic().isEmpty()) {

            List<Mono<ProductSpecificationCharacteristic>> checkingMonos = productSpecification
                    .getProductSpecCharacteristic()
                    .stream()
                    .filter(pscv -> pscv.getProductSpecCharRelationship() != null)
                    .map(pcsv -> getCheckingMono(pcsv, List.of(pcsv.getProductSpecCharRelationship())))
                    .toList();

            Mono<ProductSpecification> pscvCheckingMono = Mono.zip(checkingMonos, (m1) -> productSpecification);
            specCheckingMono = Mono.zip(specCheckingMono, pscvCheckingMono, (p1, p2) -> productSpecification);
        }

        return specCheckingMono
                .onErrorMap(throwable -> new ProductCatalogException(String.format("Was not able to create product specification %s", productSpecification.getId()), throwable, ProductCatalogExceptionReason.INVALID_RELATIONSHIP));

    }

    @Override
    public Mono<HttpResponse<Object>> deleteProductSpecification(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<ProductSpecificationVO>>> listProductSpecification(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, ProductSpecification.TYPE_PRODUCT_SPECIFICATION, ProductSpecification.class)
                .map(productSpecificationStream -> productSpecificationStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ProductSpecificationVO>> patchProductSpecification(String id, ProductSpecificationUpdateVO productSpecification) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new ProductCatalogException("Did not receive a valid id, such product specification cannot exist.", ProductCatalogExceptionReason.NOT_FOUND);
        }
        ProductSpecification updatedProductSpecification = tmForumMapper.map(tmForumMapper.map(productSpecification, id));
        updatedProductSpecification.setLastUpdate(clock.instant());
        Mono<ProductSpecification> checkingMono = getCheckingMono(updatedProductSpecification);

        Mono<ProductSpecification> productSpecCharHandlingMono = relatedEntityHandlingMono(
                updatedProductSpecification,
                checkingMono,
                updatedProductSpecification.getProductSpecCharacteristic(),
                updatedProductSpecification::setProductSpecCharacteristic,
                ProductSpecificationCharacteristic.class);

        return patch(id, updatedProductSpecification, productSpecCharHandlingMono, ProductSpecification.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ProductSpecificationVO>> retrieveProductSpecification(String id, @Nullable String fields) {
        return retrieve(id, ProductSpecification.class)
                .switchIfEmpty(Mono.error(new ProductCatalogException("No such product specification exists.", ProductCatalogExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
