package org.fiware.tmforum.resourcefunction.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import org.fiware.resourcefunction.api.HealApi;
import org.fiware.resourcefunction.model.HealCreateVO;
import org.fiware.resourcefunction.model.HealVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resourcefunction.TMForumMapper;
import org.fiware.tmforum.resourcefunction.domain.Characteristic;
import org.fiware.tmforum.resourcefunction.domain.Heal;
import org.fiware.tmforum.resourcefunction.exception.ResourceCatalogException;
import org.fiware.tmforum.resourcefunction.exception.ResourceCatalogExceptionReason;
import org.fiware.tmforum.resourcefunction.repository.ResourceCatalogRepository;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller("${general.basepath:/}")
public class HealApiController extends AbstractApiController implements HealApi {

    public HealApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ResourceCatalogRepository resourceCatalogRepository) {
        super(tmForumMapper, validationService, resourceCatalogRepository);
    }

    @Override
    public Mono<HttpResponse<HealVO>> createHeal(HealCreateVO healCreateVO) {
        Heal heal = tmForumMapper
                .map(tmForumMapper.map(healCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Heal.TYPE_HEAL)));

        Mono<Heal> checkingMono = getCheckingMono(heal);

        Mono<Heal> characteristicHandlingMono = relatedEntityHandlingMono(
                heal,
                checkingMono,
                heal.getAdditionalParms(),
                heal::setAdditionalParms,
                Characteristic.class);

        return create(characteristicHandlingMono, Heal.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<Heal> getCheckingMono(Heal heal) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();

        Optional.ofNullable(heal.getHealPolicy()).ifPresent(healPolicyRef -> references.add(List.of(healPolicyRef)));
        Optional.ofNullable(heal.getResourceFunction()).ifPresent(resourceFunctionRef -> references.add(List.of(resourceFunctionRef)));

        Mono<Heal> checkingMono = getCheckingMono(heal, references);

        if (heal.getAdditionalParms() != null && !heal.getAdditionalParms().isEmpty()) {
            List<Mono<Characteristic>> additionalParamCheckingMonos = heal
                    .getAdditionalParms()
                    .stream()
                    .filter(rc -> rc.getCharacteristicRelationship() != null)
                    .map(rc -> getCheckingMono(rc, List.of(rc.getCharacteristicRelationship())))
                    .toList();
            Mono<Heal> additionalParamCheckingMono = Mono.zip(additionalParamCheckingMonos, (m1) -> heal);
            checkingMono = Mono.zip(additionalParamCheckingMono, checkingMono, (p1, p2) -> heal);
        }

        return checkingMono
                .onErrorMap(throwable -> new ResourceCatalogException(
                        String.format("Was not able to create heal %s", heal.getId()), throwable, ResourceCatalogExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<List<HealVO>>> listHeal(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, Heal.TYPE_HEAL, Heal.class)
                .map(healStream -> healStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<HealVO>> retrieveHeal(String id, @Nullable String fields) {
        return retrieve(id, Heal.class)
                .switchIfEmpty(Mono.error(new ResourceCatalogException("No such heal exists.", ResourceCatalogExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
