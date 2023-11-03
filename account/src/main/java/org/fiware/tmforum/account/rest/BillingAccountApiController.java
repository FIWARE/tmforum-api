package org.fiware.tmforum.account.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.account.api.BillingAccountApi;
import org.fiware.account.api.BillingAccountApi;
import org.fiware.account.model.BillingAccountCreateVO;
import org.fiware.account.model.BillingAccountUpdateVO;
import org.fiware.account.model.BillingAccountVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.account.TMForumMapper;
import org.fiware.tmforum.account.domain.BillingAccount;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class BillingAccountApiController extends AbstractApiController<BillingAccount> implements BillingAccountApi {

    private final TMForumMapper tmForumMapper;

    public BillingAccountApiController(QueryParser queryParser, ReferenceValidationService validationService,
                                       TmForumRepository productBillingAccountRepository, TMForumMapper tmForumMapper, EventHandler eventHandler) {
        super(queryParser, validationService, productBillingAccountRepository, eventHandler);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<BillingAccountVO>> createBillingAccount(BillingAccountCreateVO billingAccountVo) {
        BillingAccount billingAccount = tmForumMapper.map(
                tmForumMapper.map(billingAccountVo, IdHelper.toNgsiLd(UUID.randomUUID().toString(), BillingAccount.TYPE_BILLINGAC)));

        return create(getCheckingMono(billingAccount), BillingAccount.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<BillingAccount> getCheckingMono(BillingAccount billingAccount) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(billingAccount.getRelatedParty());
        Optional.ofNullable(billingAccount.getDefaultPaymentMethod()).map(List::of).ifPresent(references::add);
        Optional.ofNullable(billingAccount.getFinancialAccount()).map(List::of).ifPresent(references::add);
        return getCheckingMono(billingAccount, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create billingAccount %s", billingAccount.getId()), throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteBillingAccount(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<BillingAccountVO>>> listBillingAccount(@Nullable String fields, @Nullable Integer offset,
                                                                 @Nullable Integer limit) {
        return list(offset, limit, BillingAccount.TYPE_BILLINGAC, BillingAccount.class)
                .map(categoryStream -> categoryStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<BillingAccountVO>> patchBillingAccount(String id, BillingAccountUpdateVO billingAccountUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException("Did not receive a valid id, such billingAccount cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }
        BillingAccount updatedBillingAccount = tmForumMapper.map(tmForumMapper.map(billingAccountUpdateVO, id));

        return patch(id, updatedBillingAccount, getCheckingMono(updatedBillingAccount), BillingAccount.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<BillingAccountVO>> retrieveBillingAccount(String id, @Nullable String fields) {
        return retrieve(id, BillingAccount.class)
                .switchIfEmpty(Mono.error(new TmForumException("No such billingAccount exists.",
                        TmForumExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}


