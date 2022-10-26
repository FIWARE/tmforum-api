package org.fiware.tmforum.resourcefunction.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import org.fiware.resourcefunction.api.HealApi;
import org.fiware.resourcefunction.model.HealCreateVO;
import org.fiware.resourcefunction.model.HealVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resource.Characteristic;
import org.fiware.tmforum.resourcefunction.TMForumMapper;
import org.fiware.tmforum.resourcefunction.domain.Heal;
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
public class HealApiController extends AbstractApiController implements HealApi {

    public HealApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ResourceFunctionRepository resourceCatalogRepository) {
        super(tmForumMapper, validationService, resourceCatalogRepository);
    }

    @Override
    public Mono<HttpResponse<HealVO>> createHeal(HealCreateVO healCreateVO) {
        Heal heal = tmForumMapper
                .map(tmForumMapper.map(healCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Heal.TYPE_HEAL)));

        return create(getCheckingMono(heal), Heal.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<Heal> getCheckingMono(Heal heal) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();

        Optional.ofNullable(heal.getHealPolicy()).ifPresent(healPolicyRef -> references.add(List.of(healPolicyRef)));
        Optional.ofNullable(heal.getResourceFunction()).ifPresent(resourceFunctionRef -> references.add(List.of(resourceFunctionRef)));

        return getCheckingMono(heal, references)
                .onErrorMap(throwable -> new ResourceFunctionException(
                        String.format("Was not able to create heal %s", heal.getId()), throwable, ResourceFunctionExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<List<HealVO>>> listHeal(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, Heal.TYPE_HEAL, Heal.class)
                .map(healStream -> healStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<HealVO>> retrieveHeal(String id, @Nullable String fields) {
        return retrieve(id, Heal.class)
                .switchIfEmpty(Mono.error(new ResourceFunctionException("No such heal exists.", ResourceFunctionExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
