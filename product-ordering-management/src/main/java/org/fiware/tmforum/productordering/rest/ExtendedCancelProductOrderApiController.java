package org.fiware.tmforum.productordering.rest;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productordering.api.ext.CancelProductOrderExtensionApi;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.productordering.domain.CancelProductOrder;
import reactor.core.publisher.Mono;

@Slf4j
@Controller("${api.product-ordering-management.basepath:/}")
@Requires(property = "apiExtension.enabled", value = "true")
public class ExtendedCancelProductOrderApiController extends AbstractApiController<CancelProductOrder>
        implements CancelProductOrderExtensionApi {

    public ExtendedCancelProductOrderApiController(
            QueryParser queryParser,
            ReferenceValidationService validationService,
            TmForumRepository repository,
            TMForumEventHandler eventHandler) {
        super(queryParser, validationService, repository, eventHandler);
    }

    @Override
    public Mono<HttpResponse<Object>> deleteCancelProductOrder(String id) {
        return delete(id);
    }
}
