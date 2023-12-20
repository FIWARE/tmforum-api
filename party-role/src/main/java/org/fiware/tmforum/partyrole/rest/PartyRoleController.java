package org.fiware.tmforum.partyrole.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.fiware.partyRole.api.PartyRoleApi;
import org.fiware.partyRole.model.PartyRoleCreateVO;
import org.fiware.partyRole.model.PartyRoleUpdateVO;
import org.fiware.partyRole.model.PartyRoleVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.partyrole.TMForumMapper;
import org.fiware.tmforum.partyrole.domain.PartyRole;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Controller("${general.basepath:/}")
public class PartyRoleController extends AbstractApiController<PartyRole> implements PartyRoleApi{
    private final TMForumMapper tmForumMapper;
    public PartyRoleController(QueryParser queryParser, ReferenceValidationService validationService, TMForumMapper mapper,TmForumRepository repository, EventHandler eventHandler) {
        super(queryParser, validationService, repository, eventHandler);
                this.tmForumMapper = mapper;
    }
    @Override
    public Mono<HttpResponse<PartyRoleVO>> createPartyRole(@NonNull PartyRoleCreateVO partyRoleCreateVO) {
     PartyRole partyRole = tmForumMapper.map(tmForumMapper.map(partyRoleCreateVO,
                                IdHelper.toNgsiLd(UUID.randomUUID().toString(), PartyRole.TYPE_PR)));
                return create(getCheckingMono(partyRole), PartyRole.class)
                                .map(tmForumMapper::map)
                                .map(HttpResponse::created);
    }
    @Override
    public Mono<HttpResponse<Object>> deletePartyRole(@NonNull String id) {
        return delete(id);    
    }
    @Override
    public Mono<HttpResponse<List<PartyRoleVO>>> listPartyRole(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        Mono<HttpResponse<List<PartyRoleVO>>> res = list(offset, limit, PartyRole.TYPE_PR, PartyRole.class)
        .map(prStream -> prStream.map(tmForumMapper::map).toList())
        .switchIfEmpty(Mono.just(List.of()))
        .map(HttpResponse::ok);
        return res;
    }
    @Override
    public Mono<HttpResponse<PartyRoleVO>> patchPartyRole(@NonNull String id, @NonNull PartyRoleUpdateVO partyRoleUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
                throw new TmForumException("Did not receive a valid id, such party role cannot exist.",
                                TmForumExceptionReason.NOT_FOUND);
        }
        PartyRole pr = tmForumMapper.map(partyRoleUpdateVO, id);

        return patch(id, pr, getCheckingMono(pr), PartyRole.class)
                        .map(tmForumMapper::map)
                        .map(HttpResponse::ok);    
    }
    @Override
    public Mono<HttpResponse<PartyRoleVO>> retrievePartyRole(@NonNull String id, @Nullable String fields) {
        return retrieve(id, PartyRole.class).switchIfEmpty(
             Mono.error(new TmForumException("No such party role exists.", TmForumExceptionReason.NOT_FOUND)))
             .map(tmForumMapper::map)
            .map(HttpResponse::ok);
    }
    
     private Mono<PartyRole> getCheckingMono(PartyRole pr) {
                List<List<? extends ReferencedEntity>> references = new ArrayList<>();
                references.add(pr.getAccount());
                references.add(pr.getAgreement());
                references.add(pr.getPaymentMethod());
                references.add(pr.getRelatedParty());
                Optional.ofNullable(pr.getEngagedParty()).map(List::of).ifPresent(references::add);
                return getCheckingMono(pr, references)
                                .onErrorMap(throwable -> new TmForumException(
                                                String.format("Was not able to create a party role %s",
                                                                pr.getId()),
                                                throwable,
                                                TmForumExceptionReason.INVALID_RELATIONSHIP));
        } 
}
