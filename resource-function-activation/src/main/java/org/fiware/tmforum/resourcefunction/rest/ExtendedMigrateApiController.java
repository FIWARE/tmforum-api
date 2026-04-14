package org.fiware.tmforum.resourcefunction.rest;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcefunction.api.ext.MigrateExtensionApi;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.resourcefunction.domain.Migrate;
import reactor.core.publisher.Mono;

@Slf4j
@Controller("${api.resource-function-activation.basepath:/}")
@Requires(property = "apiExtension.enabled", value = "true")
public class ExtendedMigrateApiController extends AbstractApiController<Migrate>
        implements MigrateExtensionApi {

    public ExtendedMigrateApiController(
            QueryParser queryParser,
            ReferenceValidationService validationService,
            TmForumRepository repository,
            TMForumEventHandler eventHandler) {
        super(queryParser, validationService, repository, eventHandler);
    }

    @Override
    public Mono<HttpResponse<Object>> deleteMigrate(String id) {
        return delete(id);
    }
}
