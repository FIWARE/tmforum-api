package org.fiware.tmforum.account.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.account.api.BillingCycleSpecificationApi;
import org.fiware.account.model.BillingCycleSpecificationCreateVO;
import org.fiware.account.model.BillingCycleSpecificationUpdateVO;
import org.fiware.account.model.BillingCycleSpecificationVO;
import org.fiware.tmforum.account.TMForumMapper;
import org.fiware.tmforum.account.domain.BillingCycleSpecification;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class BillingCycleSpecificationApiController extends AbstractApiController<BillingCycleSpecification> implements BillingCycleSpecificationApi {

    private final TMForumMapper tmForumMapper;

    public BillingCycleSpecificationApiController(QueryParser queryParser, ReferenceValidationService validationService,
                                                  TmForumRepository productBillingCycleSpecificationRepository,
                                                  TMForumMapper tmForumMapper, TMForumEventHandler eventHandler) {
        super(queryParser, validationService, productBillingCycleSpecificationRepository, eventHandler);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<BillingCycleSpecificationVO>> createBillingCycleSpecification(BillingCycleSpecificationCreateVO billingCycleSpecificationVo) {
        BillingCycleSpecification billingCycleSpecification = tmForumMapper.map(
                tmForumMapper.map(billingCycleSpecificationVo, IdHelper.toNgsiLd(UUID.randomUUID().toString(), BillingCycleSpecification.TYPE_BILLCL)));

        return create(getCheckingMono(billingCycleSpecification), BillingCycleSpecification.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<BillingCycleSpecification> getCheckingMono(BillingCycleSpecification billingCycleSpecification) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        return getCheckingMono(billingCycleSpecification, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create billingCycleSpecification %s", billingCycleSpecification.getId()), throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteBillingCycleSpecification(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<BillingCycleSpecificationVO>>> listBillingCycleSpecification(@Nullable String fields, @Nullable Integer offset,
                                                           @Nullable Integer limit) {
        return list(offset, limit, BillingCycleSpecification.TYPE_BILLCL, BillingCycleSpecification.class)
                .map(categoryStream -> categoryStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<BillingCycleSpecificationVO>> patchBillingCycleSpecification(String id, BillingCycleSpecificationUpdateVO billingCycleSpecificationUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException("Did not receive a valid id, such billingCycleSpecification cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }
        BillingCycleSpecification updatedBillingCycleSpecification = tmForumMapper.map(tmForumMapper.map(billingCycleSpecificationUpdateVO, id));

        return patch(id, updatedBillingCycleSpecification, getCheckingMono(updatedBillingCycleSpecification), BillingCycleSpecification.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<BillingCycleSpecificationVO>> retrieveBillingCycleSpecification(String id, @Nullable String fields) {
        return retrieve(id, BillingCycleSpecification.class)
                .switchIfEmpty(Mono.error(new TmForumException("No such billingCycleSpecification exists.",
                        TmForumExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}

