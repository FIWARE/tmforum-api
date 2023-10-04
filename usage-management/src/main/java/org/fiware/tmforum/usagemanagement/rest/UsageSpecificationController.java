package org.fiware.tmforum.usagemanagement.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.usagemanagement.api.UsageSpecificationApi;
import org.fiware.usagemanagement.model.UsageSpecificationCreateVO;
import org.fiware.usagemanagement.model.UsageSpecificationUpdateVO;
import org.fiware.usagemanagement.model.UsageSpecificationVO;
import org.fiware.tmforum.usagemanagement.TMForumMapper;
import org.fiware.tmforum.usagemanagement.domain.UsageSpecification;
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
public class UsageSpecificationController extends AbstractApiController<UsageSpecification>
                implements UsageSpecificationApi {

        private final TMForumMapper tmForumMapper;

        public UsageSpecificationController(ReferenceValidationService validationService, TmForumRepository repository,
                        TMForumMapper tmForumMapper, EventHandler eventHandler) {
                super(validationService, repository, eventHandler);
                this.tmForumMapper = tmForumMapper;
        }

        @Override
        public Mono<HttpResponse<UsageSpecificationVO>> createUsageSpecification(
                        @NonNull UsageSpecificationCreateVO usageSpecification) {
                UsageSpecification usage = tmForumMapper.map(tmForumMapper.map(usageSpecification,
                                IdHelper.toNgsiLd(UUID.randomUUID().toString(), UsageSpecification.TYPE_USP)));
                return create(getCheckingMono(usage), UsageSpecification.class)
                                .map(tmForumMapper::map)
                                .map(HttpResponse::created);
        }

        private Mono<UsageSpecification> getCheckingMono(UsageSpecification ug) {
                List<List<? extends ReferencedEntity>> references = new ArrayList<>();
                references.add(ug.getEntitySpecRelationship());
                references.add(ug.getConstraint());
                references.add(ug.getRelatedParty());
                return getCheckingMono(ug, references)
                                .onErrorMap(throwable -> new TmForumException(
                                                String.format("Was not able to create usage specification %s",
                                                                ug.getId()),
                                                throwable,
                                                TmForumExceptionReason.INVALID_RELATIONSHIP));
        }

        @Override
        public Mono<HttpResponse<Object>> deleteUsageSpecification(@NonNull String id) {
                return delete(id);
        }

        @Override
        public Mono<HttpResponse<List<UsageSpecificationVO>>> listUsageSpecification(@Nullable String fields,
                        @Nullable Integer offset,
                        @Nullable Integer limit) {
                Mono<HttpResponse<List<UsageSpecificationVO>>> res = list(offset, limit,
                                UsageSpecification.TYPE_USP,
                                UsageSpecification.class)
                                .map(agStream -> agStream.map(tmForumMapper::map).toList())
                                .switchIfEmpty(Mono.just(List.of()))
                                .map(HttpResponse::ok);
                return res;
        }

        @Override
        public Mono<HttpResponse<UsageSpecificationVO>> patchUsageSpecification(@NonNull String id,
                        @NonNull UsageSpecificationUpdateVO usageSpecification) {
                if (!IdHelper.isNgsiLdId(id)) {
                        throw new TmForumException("Did not receive a valid id, such usage specification cannot exist.",
                                        TmForumExceptionReason.NOT_FOUND);
                }
                UsageSpecification ug = tmForumMapper.map(usageSpecification, id);

                return patch(id, ug, getCheckingMono(ug), UsageSpecification.class)
                                .map(tmForumMapper::map)
                                .map(HttpResponse::ok);
        }

        @Override
        public Mono<HttpResponse<UsageSpecificationVO>> retrieveUsageSpecification(@NonNull String id,
                        @Nullable String fields) {
                return retrieve(id, UsageSpecification.class)
                                .switchIfEmpty(
                                                Mono.error(
                                                                new TmForumException("No such usage specification exists.",
                                                                                TmForumExceptionReason.NOT_FOUND)))
                                .map(tmForumMapper::map)
                                .map(HttpResponse::ok);
        }

}

