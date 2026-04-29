package org.fiware.tmforum.resourcefunction.rest;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcefunction.api.ext.HealExtensionApi;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.resourcefunction.domain.Heal;
import reactor.core.publisher.Mono;

@Slf4j
@Controller("${api.resource-function-activation.basepath:/}")
@Requires(property = "apiExtension.enabled", value = "true")
public class ExtendedHealApiController extends AbstractApiController<Heal>
        implements HealExtensionApi {

    public ExtendedHealApiController(
            QueryParser queryParser,
            ReferenceValidationService validationService,
            TmForumRepository repository,
            TMForumEventHandler eventHandler) {
        super(queryParser, validationService, repository, eventHandler);
    }

    @Override
    public Mono<HttpResponse<Object>> deleteHeal(String id) {
        return delete(id);
    }
}
