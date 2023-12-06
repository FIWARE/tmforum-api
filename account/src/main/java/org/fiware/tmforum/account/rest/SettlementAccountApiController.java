package org.fiware.tmforum.account.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.account.api.SettlementAccountApi;
import org.fiware.account.model.SettlementAccountCreateVO;
import org.fiware.account.model.SettlementAccountUpdateVO;
import org.fiware.account.model.SettlementAccountVO;
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
import org.fiware.tmforum.account.domain.SettlementAccount;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class SettlementAccountApiController extends AbstractApiController<SettlementAccount> implements SettlementAccountApi {

    private final TMForumMapper tmForumMapper;


    public SettlementAccountApiController(QueryParser queryParser, ReferenceValidationService validationService,
                                          TmForumRepository productSettlementAccountRepository, TMForumMapper tmForumMapper, EventHandler eventHandler) {
        super(queryParser, validationService, productSettlementAccountRepository, eventHandler);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<SettlementAccountVO>> createSettlementAccount(SettlementAccountCreateVO settlementAccountVo) {
        SettlementAccount settlementAccount = tmForumMapper.map(
                tmForumMapper.map(settlementAccountVo, IdHelper.toNgsiLd(UUID.randomUUID().toString(), SettlementAccount.TYPE_SETTLEMENTAC)));

        return create(getCheckingMono(settlementAccount), SettlementAccount.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<SettlementAccount> getCheckingMono(SettlementAccount settlementAccount) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(settlementAccount.getRelatedParty());
        Optional.ofNullable(settlementAccount.getDefaultPaymentMethod()).map(List::of).ifPresent(references::add);
        Optional.ofNullable(settlementAccount.getFinancialAccount()).map(List::of).ifPresent(references::add);
        return getCheckingMono(settlementAccount, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create settlementAccount %s", settlementAccount.getId()), throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteSettlementAccount(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<SettlementAccountVO>>> listSettlementAccount(@Nullable String fields, @Nullable Integer offset,
                                                                     @Nullable Integer limit) {
        return list(offset, limit, SettlementAccount.TYPE_SETTLEMENTAC, SettlementAccount.class)
                .map(settlementAccountStream -> settlementAccountStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<SettlementAccountVO>> patchSettlementAccount(String id, SettlementAccountUpdateVO settlementAccountUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException("Did not receive a valid id, such settlementAccount cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }
        SettlementAccount updatedSettlementAccount = tmForumMapper.map(tmForumMapper.map(settlementAccountUpdateVO, id));

        return patch(id, updatedSettlementAccount, getCheckingMono(updatedSettlementAccount), SettlementAccount.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<SettlementAccountVO>> retrieveSettlementAccount(String id, @Nullable String fields) {
        return retrieve(id, SettlementAccount.class)
                .switchIfEmpty(Mono.error(new TmForumException("No such settlementAccount exists.",
                        TmForumExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}

