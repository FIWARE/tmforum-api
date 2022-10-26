package org.fiware.tmforum.resourcefunction.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import org.fiware.resourcefunction.api.ScaleApi;
import org.fiware.resourcefunction.model.ScaleCreateVO;
import org.fiware.resourcefunction.model.ScaleVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resourcefunction.TMForumMapper;
import org.fiware.tmforum.resourcefunction.domain.Scale;
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
public class ScaleApiController extends AbstractApiController implements ScaleApi {

    public ScaleApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, ResourceFunctionRepository resourceCatalogRepository) {
        super(tmForumMapper, validationService, resourceCatalogRepository);
    }

    @Override
    public Mono<HttpResponse<ScaleVO>> createScale(ScaleCreateVO scaleCreateVO) {
        Scale scale = tmForumMapper
                .map(tmForumMapper.map(scaleCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Scale.TYPE_SCALE)));

        Mono<Scale> checkingMono = getCheckingMono(scale);

        return create(checkingMono, Scale.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);

    }

    private Mono<Scale> getCheckingMono(Scale scale) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();

        references.add(scale.getSchedule());
        Optional.ofNullable(scale.getResourceFunction()).ifPresent(resourceFunctionRef -> references.add(List.of(resourceFunctionRef)));

        return getCheckingMono(scale, references)
                .onErrorMap(throwable -> new ResourceFunctionException(
                        String.format("Was not able to create scale %s", scale.getId()), throwable, ResourceFunctionExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<List<ScaleVO>>> listScale(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, Scale.TYPE_SCALE, Scale.class)
                .map(scaleStream -> scaleStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<ScaleVO>> retrieveScale(String id, @Nullable String fields) {
        return retrieve(id, Scale.class)
                .switchIfEmpty(Mono.error(new ResourceFunctionException("No such scale exists.", ResourceFunctionExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
