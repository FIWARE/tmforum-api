package org.fiware.tmforum.usagemanagement.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.fiware.usagemanagement.api.UsageApi;
import org.fiware.usagemanagement.model.UsageCreateVO;
import org.fiware.usagemanagement.model.UsageUpdateVO;
import org.fiware.usagemanagement.model.UsageVO;
import org.fiware.tmforum.usagemanagement.TMForumMapper;
import org.fiware.tmforum.usagemanagement.domain.Usage;
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
public class UsageController extends AbstractApiController<Usage> implements UsageApi {

        private final TMForumMapper tmForumMapper;

        public UsageController(ReferenceValidationService validationService, TmForumRepository partyRepository,
                        TMForumMapper tmForumMapper) {
                super(validationService, partyRepository);
                this.tmForumMapper = tmForumMapper;
        }

        @Override
        public Mono<HttpResponse<UsageVO>> createUsage(@NonNull UsageCreateVO usageCreateVO) {
                Usage usage = tmForumMapper.map(tmForumMapper.map(usageCreateVO,
                                IdHelper.toNgsiLd(UUID.randomUUID().toString(), Usage.TYPE_U)));
                return create(getCheckingMono(usage), Usage.class)
                                .map(tmForumMapper::map)
                                .map(HttpResponse::created);
        }

        private Mono<Usage> getCheckingMono(Usage ug) {
                List<List<? extends ReferencedEntity>> references = new ArrayList<>();
                references.add(ug.getRelatedParty());
                Optional.ofNullable(ug.getUsageSpecification()).map(List::of).ifPresent(references::add);
                return getCheckingMono(ug, references)
                                .onErrorMap(throwable -> new TmForumException(
                                                String.format("Was not able to create usage %s",
                                                                ug.getId()),
                                                throwable,
                                                TmForumExceptionReason.INVALID_RELATIONSHIP));
        }

        @Override
        public Mono<HttpResponse<Object>> deleteUsage(@NonNull String id) {
                return delete(id);
        }

        @Override
        public Mono<HttpResponse<List<UsageVO>>> listUsage(@Nullable String fields, @Nullable Integer offset,
                        @Nullable Integer limit) {
                Mono<HttpResponse<List<UsageVO>>> res = list(offset, limit, Usage.TYPE_U, Usage.class)
                                .map(ugStream -> ugStream.map(tmForumMapper::map).toList())
                                .switchIfEmpty(Mono.just(List.of()))
                                .map(HttpResponse::ok);
                return res;
        }

        @Override
        public Mono<HttpResponse<UsageVO>> patchUsage(@NonNull String id,
                        @NonNull UsageUpdateVO usageUpdateVO) {
                // non-ngsi-ld ids cannot exist.
                if (!IdHelper.isNgsiLdId(id)) {
                        throw new TmForumException("Did not receive a valid id, such usage cannot exist.",
                                        TmForumExceptionReason.NOT_FOUND);
                }
                Usage ug = tmForumMapper.map(usageUpdateVO, id);

                return patch(id, ug, getCheckingMono(ug), Usage.class)
                                .map(tmForumMapper::map)
                                .map(HttpResponse::ok);
        }

        @Override
        public Mono<HttpResponse<UsageVO>> retrieveUsage(@NonNull String id, @Nullable String fields) {
                return retrieve(id, Usage.class)
                                .switchIfEmpty(
                                                Mono.error(
                                                                new TmForumException("No such usage exists.", /* fallo  */
                                                                                TmForumExceptionReason.NOT_FOUND)))
                                .map(tmForumMapper::map)
                                .map(HttpResponse::ok);
        }
}
