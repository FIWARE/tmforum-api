package org.fiware.tmforum.account.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.account.api.PartyAccountApi;
import org.fiware.account.model.PartyAccountCreateVO;
import org.fiware.account.model.PartyAccountUpdateVO;
import org.fiware.account.model.PartyAccountVO;
import org.fiware.tmforum.account.TMForumMapper;
import org.fiware.tmforum.account.domain.PartyAccount;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class PartyAccountApiController extends AbstractApiController<PartyAccount> implements PartyAccountApi {

    private final TMForumMapper tmForumMapper;


    public PartyAccountApiController(QueryParser queryParser, ReferenceValidationService validationService,
                                     TmForumRepository productPartyAccountRepository, TMForumMapper tmForumMapper,
                                     TMForumEventHandler eventHandler) {
        super(queryParser, validationService, productPartyAccountRepository, eventHandler);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<PartyAccountVO>> createPartyAccount(PartyAccountCreateVO partyAccountVo) {
        PartyAccount partyAccount = tmForumMapper.map(
                tmForumMapper.map(partyAccountVo, IdHelper.toNgsiLd(UUID.randomUUID().toString(), PartyAccount.TYPE_PARTYAC)));

        return create(getCheckingMono(partyAccount), PartyAccount.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<PartyAccount> getCheckingMono(PartyAccount partyAccount) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(partyAccount.getRelatedParty());
        Optional.ofNullable(partyAccount.getDefaultPaymentMethod()).map(List::of).ifPresent(references::add);
        Optional.ofNullable(partyAccount.getFinancialAccount()).map(List::of).ifPresent(references::add);
        return getCheckingMono(partyAccount, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create partyAccount %s", partyAccount.getId()), throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deletePartyAccount(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<PartyAccountVO>>> listPartyAccount(@Nullable String fields, @Nullable Integer offset,
                                                                             @Nullable Integer limit) {
        return list(offset, limit, PartyAccount.TYPE_PARTYAC, PartyAccount.class)
                .map(partyAccountStream -> partyAccountStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<PartyAccountVO>> patchPartyAccount(String id, PartyAccountUpdateVO partyAccountUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException("Did not receive a valid id, such partyAccount cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }
        PartyAccount updatedPartyAccount = tmForumMapper.map(tmForumMapper.map(partyAccountUpdateVO, id));

        return patch(id, updatedPartyAccount, getCheckingMono(updatedPartyAccount), PartyAccount.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<PartyAccountVO>> retrievePartyAccount(String id, @Nullable String fields) {
        return retrieve(id, PartyAccount.class)
                .switchIfEmpty(Mono.error(new TmForumException("No such partyAccount exists.",
                        TmForumExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}

