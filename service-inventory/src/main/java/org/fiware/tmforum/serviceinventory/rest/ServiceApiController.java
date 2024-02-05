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
import org.fiware.tmforum.common.domain.ConstraintRef;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.service.*;
import org.fiware.tmforum.resource.*;
import org.fiware.tmforum.serviceinventory.domain.Service;
import org.fiware.tmforum.serviceinventory.TMForumMapper;
import org.fiware.tmforum.serviceinventory.domain.ServiceRelationship;
import reactor.core.publisher.Mono;

import java.net.URI;
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
        references.add(service.getSupportingService());
        references.add(service.getRelatedParty());
        Optional.ofNullable(service.getServiceSpecification()).map(List::of).ifPresent(references::add);
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

        if (service.getFeature() != null && !service.getFeature().isEmpty()) {
            List<Mono<Service>> featureConstraintsCheckingMonos = service.getFeature()
                    .stream()
                    .peek(feature -> validateInternalFeatureRefs(feature, service))
                    .filter(feature -> feature.getConstraint() != null)
                    .map(feature -> getCheckingMono(service, List.of(feature.getConstraint())))
                    .toList();
            if (!featureConstraintsCheckingMonos.isEmpty()) {
                Mono<Service> featureConstraintsCheckingMono = Mono.zip(featureConstraintsCheckingMonos,
                        p -> service);
                checkingMono = Mono.zip(featureConstraintsCheckingMono, checkingMono, (p1, p2) -> service);
            }
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

    private void validateInternalRefs(Service service) {
        if (service.getNote() != null) {
            List<URI> noteIds = service.getNote().stream().map(Note::getId).toList();
            if (noteIds.size() != new HashSet<>(noteIds).size()) {
                throw new TmForumException(
                        String.format("Duplicate note ids are not allowed: %s", noteIds),
                        TmForumExceptionReason.INVALID_DATA);
            }
        }
        if (service.getServiceCharacteristic() != null) {
            service.getServiceCharacteristic()
                    .forEach(characteristic -> validateInternalCharacteristicRefs(characteristic,
                            service.getServiceCharacteristic()));
        }

    }

    private void validateInternalCharacteristicRefs(Characteristic characteristic,
                                                    List<Characteristic> characteristics) {
        List<String> charIds = characteristics
                .stream()
                .map(Characteristic::getId)
                .toList();
        if (charIds.size() != new HashSet<>(charIds).size()) {
            throw new TmForumException(
                    String.format("Duplicate characteristic ids are not allowed: %s", charIds),
                    TmForumExceptionReason.INVALID_DATA);
        }

        if (characteristic.getCharacteristicRelationship() != null) {
            characteristic.getCharacteristicRelationship()
                    .stream()
                    .map(CharacteristicRelationship::getId)
                    .filter(charRef -> !charIds.contains(charRef))
                    .findFirst()
                    .ifPresent(missingId -> {
                        throw new TmForumException(
                                String.format("Referenced characteristic %s does not exist", missingId),
                                TmForumExceptionReason.INVALID_DATA);
                    });
        }
    }


    private void validateInternalFeatureRefs(Feature feature, Service service) {
        List<String> featureIds = service.getFeature()
                .stream()
                .map(Feature::getId)
                .toList();
        // check for duplicate ids
        if (featureIds.size() != new HashSet<>(featureIds).size()) {
            throw new TmForumException(String.format("Duplicate feature ids are not allowed: %s", featureIds),
                    TmForumExceptionReason.INVALID_DATA);
        }
        if (feature.getFeatureRelationship() != null) {
            feature.getFeatureRelationship()
                    .stream()
                    .map(FeatureRelationship::getId)
                    .filter(featureRef -> !featureIds.contains(featureRef))
                    .findFirst()
                    .ifPresent(missingId -> {
                        throw new TmForumException(
                                String.format("Referenced feature %s does not exist", missingId),
                                TmForumExceptionReason.INVALID_DATA);
                    });
        }
        if (feature.getFeatureCharacteristic() != null) {
            feature.getFeatureCharacteristic()
                    .forEach(characteristic -> validateInternalCharacteristicRefs(characteristic,
                            feature.getFeatureCharacteristic()));
        }
    }
}
