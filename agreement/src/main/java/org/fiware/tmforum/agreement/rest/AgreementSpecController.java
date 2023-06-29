package org.fiware.tmforum.agreement.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.fiware.agreement.api.AgreementSpecificationApi;
import org.fiware.agreement.model.AgreementSpecificationCreateVO;
import org.fiware.agreement.model.AgreementSpecificationUpdateVO;
import org.fiware.agreement.model.AgreementSpecificationVO;
import org.fiware.tmforum.agreement.TMForumMapper;
import org.fiware.tmforum.agreement.domain.AgreementSpecification;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Controller("${general.basepath:/}")
public class AgreementSpecController extends AbstractApiController<AgreementSpecification>
                implements AgreementSpecificationApi {

        private final TMForumMapper tmForumMapper;

        public AgreementSpecController(ReferenceValidationService validationService, TmForumRepository repository,
                        TMForumMapper tmForumMapper) {
                super(validationService, repository);
                this.tmForumMapper = tmForumMapper;
        }

        @Override
        public Mono<HttpResponse<AgreementSpecificationVO>> createAgreementSpecification(
                        @NonNull AgreementSpecificationCreateVO agreementSpecification) {
                AgreementSpecification agreement = tmForumMapper.map(tmForumMapper.map(agreementSpecification,
                                IdHelper.toNgsiLd(UUID.randomUUID().toString(), AgreementSpecification.TYPE_AGSP)));
                return create(getCheckingMono(agreement), AgreementSpecification.class)
                                .map(tmForumMapper::map)
                                .map(HttpResponse::created);
        }

        private Mono<AgreementSpecification> getCheckingMono(AgreementSpecification ag) {
                List<List<? extends ReferencedEntity>> references = new ArrayList<>();
                references.add(ag.getRelatedParty());
                references.add(ag.getSpecificationRelationship());
                Optional.ofNullable(ag.getServiceCategory()).map(List::of).ifPresent(references::add);
                return getCheckingMono(ag, references)
                                .onErrorMap(throwable -> new TmForumException(
                                                String.format("Was not able to create agreement specification %s",
                                                                ag.getId()),
                                                throwable,
                                                TmForumExceptionReason.INVALID_RELATIONSHIP));
        }

        @Override
        public Mono<HttpResponse<Object>> deleteAgreementSpecification(@NonNull String id) {
                return delete(id);
        }

        @Override
        public Mono<HttpResponse<List<AgreementSpecificationVO>>> listAgreementSpecification(@Nullable String fields,
                        @Nullable Integer offset,
                        @Nullable Integer limit) {
                Mono<HttpResponse<List<AgreementSpecificationVO>>> res = list(offset, limit,
                                AgreementSpecification.TYPE_AGSP,
                                AgreementSpecification.class)
                                .map(agStream -> agStream.map(tmForumMapper::map).toList())
                                .switchIfEmpty(Mono.just(List.of()))
                                .map(HttpResponse::ok);
                return res;
        }

        @Override
        public Mono<HttpResponse<AgreementSpecificationVO>> patchAgreementSpecification(@NonNull String id,
                        @NonNull AgreementSpecificationUpdateVO agreementSpecification) {
                if (!IdHelper.isNgsiLdId(id)) {
                        throw new TmForumException("Did not receive a valid id, such agreement cannot exist.",
                                        TmForumExceptionReason.NOT_FOUND);
                }
                AgreementSpecification ag = tmForumMapper.map(agreementSpecification, id);

                return patch(id, ag, getCheckingMono(ag), AgreementSpecification.class)
                                .map(tmForumMapper::map)
                                .map(HttpResponse::ok);
        }

        @Override
        public Mono<HttpResponse<AgreementSpecificationVO>> retrieveAgreementSpecification(@NonNull String id,
                        @Nullable String fields) {
                return retrieve(id, AgreementSpecification.class)
                                .switchIfEmpty(
                                                Mono.error(
                                                                new TmForumException("No such individual exists.",
                                                                                TmForumExceptionReason.NOT_FOUND)))
                                .map(tmForumMapper::map)
                                .map(HttpResponse::ok);
        }

}
