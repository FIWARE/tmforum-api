package org.fiware.tmforum.resourcefunction.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import org.fiware.resourcefunction.api.MigrateApi;
import org.fiware.resourcefunction.model.MigrateCreateVO;
import org.fiware.resourcefunction.model.MigrateVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resource.Characteristic;
import org.fiware.tmforum.resourcefunction.TMForumMapper;
import org.fiware.tmforum.resourcefunction.domain.Migrate;
import org.fiware.tmforum.resourcefunction.exception.ResourceFunctionException;
import org.fiware.tmforum.resourcefunction.exception.ResourceFunctionExceptionReason;
import org.fiware.tmforum.resourcefunction.repository.ResourceFunctionRepository;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller("${general.basepath:/}")
public class MigrateApiController extends AbstractApiController implements MigrateApi {

    public MigrateApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ResourceFunctionRepository resourceCatalogRepository) {
        super(tmForumMapper, validationService, resourceCatalogRepository);
    }

    @Override
    public Mono<HttpResponse<MigrateVO>> createMigrate(MigrateCreateVO migrateCreateVO) {
        Migrate migrate = tmForumMapper
                .map(tmForumMapper.map(migrateCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Migrate.TYPE_MIGRATE)));

        Mono<Migrate> checkingMono = getCheckingMono(migrate);

        Mono<Migrate> characteristicHandlingMono = relatedEntityHandlingMono(
                migrate,
                checkingMono,
                migrate.getCharacteristics(),
                migrate::setCharacteristics,
                Characteristic.class);

        return create(characteristicHandlingMono, Migrate.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<Migrate> getCheckingMono(Migrate migrate) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();

        references.add(migrate.getAddConnectionPoint());
        references.add(migrate.getRemoveConnectionPoint());

        Optional.ofNullable(migrate.getPlace()).ifPresent(placeRef -> references.add(List.of(placeRef)));
        Optional.ofNullable(migrate.getResourceFunction()).ifPresent(resourceFunctionRef -> references.add(List.of(resourceFunctionRef)));

        Mono<Migrate> checkingMono = getCheckingMono(migrate, references);

        if (migrate.getCharacteristics() != null && !migrate.getCharacteristics().isEmpty()) {
            List<Mono<Characteristic>> characteristicsCheckingMonos = migrate
                    .getCharacteristics()
                    .stream()
                    .filter(rc -> rc.getCharacteristicRelationship() != null)
                    .map(rc -> getCheckingMono(rc, List.of(rc.getCharacteristicRelationship())))
                    .toList();
            Mono<Migrate> characteristicsCheckingMono = Mono.zip(characteristicsCheckingMonos, (m1) -> migrate);
            checkingMono = Mono.zip(characteristicsCheckingMono, checkingMono, (p1, p2) -> migrate);
        }
        return checkingMono
                .onErrorMap(throwable -> new ResourceFunctionException(
                        String.format("Was not able to create migrate %s", migrate.getId()), throwable, ResourceFunctionExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<List<MigrateVO>>> listMigrate(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, Migrate.TYPE_MIGRATE, Migrate.class)
                .map(migrateStream -> migrateStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<MigrateVO>> retrieveMigrate(String id, @Nullable String fields) {
        return retrieve(id, Migrate.class)
                .switchIfEmpty(Mono.error(new ResourceFunctionException("No such migrate exists.", ResourceFunctionExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
