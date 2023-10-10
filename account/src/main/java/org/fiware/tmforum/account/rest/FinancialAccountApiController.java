package org.fiware.tmforum.account.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.account.api.FinancialAccountApi;
import org.fiware.account.model.FinancialAccountCreateVO;
import org.fiware.account.model.FinancialAccountUpdateVO;
import org.fiware.account.model.FinancialAccountVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.account.TMForumMapper;
import org.fiware.tmforum.account.domain.FinancialAccount;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class FinancialAccountApiController extends AbstractApiController<FinancialAccount> implements FinancialAccountApi {

    private final TMForumMapper tmForumMapper;

    public FinancialAccountApiController(ReferenceValidationService validationService,
                                              TmForumRepository productFinancialAccountRepository, TMForumMapper tmForumMapper, EventHandler eventHandler) {
        super(validationService, productFinancialAccountRepository, eventHandler);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<FinancialAccountVO>> createFinancialAccount(FinancialAccountCreateVO financialAccountVo) {
        FinancialAccount financialAccount = tmForumMapper.map(
                tmForumMapper.map(financialAccountVo, IdHelper.toNgsiLd(UUID.randomUUID().toString(), FinancialAccount.TYPE_FINANCIALAC)));

        return create(getCheckingMono(financialAccount), FinancialAccount.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<FinancialAccount> getCheckingMono(FinancialAccount financialAccount) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        return getCheckingMono(financialAccount, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create financialAccount %s", financialAccount.getId()), throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteFinancialAccount(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<FinancialAccountVO>>> listFinancialAccount(@Nullable String fields, @Nullable Integer offset,
                                                                                       @Nullable Integer limit) {
        return list(offset, limit, FinancialAccount.TYPE_FINANCIALAC, FinancialAccount.class)
                .map(categoryStream -> categoryStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<FinancialAccountVO>> patchFinancialAccount(String id, FinancialAccountUpdateVO financialAccountUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException("Did not receive a valid id, such financialAccount cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }
        FinancialAccount updatedFinancialAccount = tmForumMapper.map(tmForumMapper.map(financialAccountUpdateVO, id));

        return patch(id, updatedFinancialAccount, getCheckingMono(updatedFinancialAccount), FinancialAccount.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<FinancialAccountVO>> retrieveFinancialAccount(String id, @Nullable String fields) {
        return retrieve(id, FinancialAccount.class)
                .switchIfEmpty(Mono.error(new TmForumException("No such financialAccount exists.",
                        TmForumExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}

