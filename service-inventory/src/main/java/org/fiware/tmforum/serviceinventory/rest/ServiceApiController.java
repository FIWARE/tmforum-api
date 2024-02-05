package org.fiware.tmforum.serviceinventory.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.serviceinventory.api.ServiceApi;
import org.fiware.serviceinventory.model.ServiceCreateVO;
import org.fiware.serviceinventory.model.ServiceUpdateVO;
import org.fiware.serviceinventory.model.ServiceVO;
import org.fiware.tmforum.common.domain.BillingAccountRef;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.serviceinventory.domain.Service;
import org.fiware.tmforum.serviceinventory.TMForumMapper;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Controller("${general.basepath:/}")
public class ServiceApiController extends AbstractApiController<Service> implements ServiceApi {

    private final TMForumMapper tmForumMapper;

    public ServiceApiController(QueryParser queryParser, ReferenceValidationService validationService,
                                TmForumRepository repository, TMForumMapper tmForumMapper, TMForumEventHandler eventHandler) {
        super(queryParser, validationService, repository, eventHandler);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<ServiceVO>> createService(@NonNull ServiceCreateVO serviceCreateVO) {
        Service service = tmForumMapper.map(
                tmForumMapper.map(serviceCreateVO,
                        IdHelper.toNgsiLd(UUID.randomUUID().toString(), Service.TYPE_SERVICE)));

        return create(getCheckingMono(service), Service.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<Service> getCheckingMono(Service service) {

        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(service.getAgreement());
        references.add(service.getPlace());
        references.add(service.getService());
        references.add(service.getServiceOrderItem());
        references.add(service.getRealizingResource());
        references.add(service.getRealizingService());
        references.add(service.getRelatedParty());
        Optional.ofNullable(service.getBillingAccount()).map(List::of).ifPresent(references::add);
        Optional.ofNullable(service.getServiceOffering()).map(List::of).ifPresent(references::add);
        Optional.ofNullable(service.getServiceSpecification()).map(List::of).ifPresent(references::add);

        Mono<Service> checkingMono = getCheckingMono(service, references);

        if (service.getServiceRelationship() != null && !service.getServiceRelationship().isEmpty()) {
            List<Mono<Service>> serviceRelCheckingMonos = service.getServiceRelationship()
                    .stream()
                    .map(ServiceRelationship::getService)
                    .map(serviceRef -> getCheckingMono(service, List.of(List.of(serviceRef))))
                    .toList();
            if (!serviceRelCheckingMonos.isEmpty()) {
                Mono<Service> serviceRelCheckingMono = Mono.zip(serviceRelCheckingMonos, p -> service);
                checkingMono = Mono.zip(serviceRelCheckingMono, checkingMono, (p1, p2) -> service);
            }
        }

        if (service.getServicePrice() != null && !service.getServicePrice().isEmpty()) {
            List<List<? extends ReferencedEntity>> internalReferences = new ArrayList<>();

            List<BillingAccountRef> billingAccountRefs = service.getServicePrice()
                    .stream()
                    .map(ServicePrice::getBillingAccount)
                    .filter(Objects::nonNull)
                    .toList();
            internalReferences.add(billingAccountRefs);
            List<ServiceOfferingPriceRefValue> serviceOfferingPriceRefs = service.getServicePrice()
                    .stream()
                    .map(ServicePrice::getServiceOfferingPrice)
                    .filter(Objects::nonNull)
                    .toList();
            internalReferences.add(serviceOfferingPriceRefs);
            List<ServiceOfferingPriceRef> alterationRefs = service.getServicePrice()
                    .stream()
                    .map(ServicePrice::getServicePriceAlteration)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .map(PriceAlteration::getServiceOfferingPrice)
                    .filter(Objects::nonNull)
                    .toList();
            internalReferences.add(alterationRefs);
            checkingMono = Mono.zip(getCheckingMono(service, internalReferences), checkingMono, (p1, p2) -> service);
        }
        return checkingMono
                .onErrorMap(throwable ->
                        new TmForumException(
                                String.format("Was not able to create service %s", service.getId()),
                                throwable,
                                TmForumExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override public Mono<HttpResponse<Object>> deleteService(@NonNull String id) {
        return delete(id);
    }

    @Override public Mono<HttpResponse<List<ServiceVO>>> listService(@Nullable String fields, @Nullable Integer offset,
                                                                     @Nullable Integer limit) {
        return list(offset, limit, Service.TYPE_SERVICE, Service.class)
                .map(serviceStream -> serviceStream
                        .map(tmForumMapper::map)
                        .toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override public Mono<HttpResponse<ServiceVO>> patchService(@NonNull String id,
                                                                @NonNull ServiceUpdateVO serviceUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException("Did not receive a valid id, such service cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }

        Service service = tmForumMapper.map(serviceUpdateVO, id);

        return patch(id, service, getCheckingMono(service), Service.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override public Mono<HttpResponse<ServiceVO>> retrieveService(@NonNull String id, @Nullable String fields) {
        return retrieve(id, Service.class)
                .switchIfEmpty(Mono.error(new TmForumException("No such service exists.",
                        TmForumExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
