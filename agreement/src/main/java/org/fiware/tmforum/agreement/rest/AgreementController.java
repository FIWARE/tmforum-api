package org.fiware.tmforum.agreement.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.fiware.agreement.api.AgreementApi;
import org.fiware.agreement.model.AgreementCreateVO;
import org.fiware.agreement.model.AgreementUpdateVO;
import org.fiware.agreement.model.AgreementVO;
import org.fiware.tmforum.agreement.domain.Agreement;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import reactor.core.publisher.Mono;
import org.fiware.tmforum.agreement.TMForumMapper;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;

@Slf4j
@Controller("${general.basepath:/}")
public class AgreementController extends AbstractApiController<Agreement> implements AgreementApi {

        private final TMForumMapper tmForumMapper;

        public AgreementController(ReferenceValidationService validationService, TmForumRepository partyRepository,
                        TMForumMapper tmForumMapper) {
                super(validationService, partyRepository);
                this.tmForumMapper = tmForumMapper;
        }

        @Override
        public Mono<HttpResponse<AgreementVO>> createAgreement(AgreementCreateVO agreementCreateVO) {
                Agreement agreement = tmForumMapper.map(tmForumMapper.map(agreementCreateVO,
                                IdHelper.toNgsiLd(UUID.randomUUID().toString(), Agreement.TYPE_AG)));
                return create(getCheckingMono(agreement), Agreement.class)
                                .map(tmForumMapper::map)
                                .map(HttpResponse::created);
        }

        private Mono<Agreement> getCheckingMono(Agreement ag) {
                List<List<? extends ReferencedEntity>> references = new ArrayList<>();
                references.add(ag.getEngagedParty());
                references.add(ag.getAssociatedAgreement());
                return getCheckingMono(ag, references)
                                .onErrorMap(throwable -> new TmForumException(
                                                String.format("Was not able to create individual %s",
                                                                ag.getId()),
                                                throwable,
                                                TmForumExceptionReason.INVALID_RELATIONSHIP));
        }

        @Override
        public Mono<HttpResponse<Object>> deleteAgreement(String id) {
                return delete(id);
        }

        @Override
        public Mono<HttpResponse<List<AgreementVO>>> listAgreement(@Nullable String fields, @Nullable Integer offset,
                        @Nullable Integer limit) {
                Mono<HttpResponse<List<AgreementVO>>> res = list(offset, limit, Agreement.TYPE_AG, Agreement.class)
                                .map(agStream -> agStream.map(tmForumMapper::map).toList())
                                .switchIfEmpty(Mono.just(List.of()))
                                .map(HttpResponse::ok);
                return res;
        }

        @Override
        public Mono<HttpResponse<AgreementVO>> patchAgreement(String id, AgreementUpdateVO agreementUpdateVO) {
                // non-ngsi-ld ids cannot exist.
                if (!IdHelper.isNgsiLdId(id)) {
                        throw new TmForumException("Did not receive a valid id, such agreement cannot exist.",
                                        TmForumExceptionReason.NOT_FOUND);
                }
                Agreement ag = tmForumMapper.map(agreementUpdateVO, id);

                return patch(id, ag, getCheckingMono(ag), Agreement.class)
                                .map(tmForumMapper::map)
                                .map(HttpResponse::ok);
        }

        @Override
        public Mono<HttpResponse<AgreementVO>> retrieveAgreement(String id, String fields) {
                return retrieve(id, Agreement.class)
                                .switchIfEmpty(
                                                Mono.error(
                                                                new TmForumException("No such individual exists.",
                                                                                TmForumExceptionReason.NOT_FOUND)))
                                .map(tmForumMapper::map)
                                .map(HttpResponse::ok);
        }
}