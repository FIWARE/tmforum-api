package org.fiware.tmforum.agreement.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.agreement.api.AgreementApi;
import org.fiware.agreement.model.AgreementCreateVO;
import org.fiware.agreement.model.AgreementUpdateVO;
import org.fiware.agreement.model.AgreementVO;
import org.fiware.tmforum.agreement.TMForumMapper;
import org.fiware.tmforum.agreement.domain.Agreement;
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
public class AgreementController extends AbstractApiController<Agreement> implements AgreementApi {

	private final TMForumMapper tmForumMapper;

	public AgreementController(QueryParser queryParser, ReferenceValidationService validationService, TmForumRepository partyRepository,
							   TMForumMapper tmForumMapper, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, partyRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<AgreementVO>> createAgreement(@NonNull AgreementCreateVO agreementCreateVO) {
		Agreement agreement = tmForumMapper.map(tmForumMapper.map(agreementCreateVO,
				IdHelper.toNgsiLd(UUID.randomUUID().toString(), Agreement.TYPE_AGREEMENT)));
		return create(getCheckingMono(agreement), Agreement.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<Agreement> getCheckingMono(Agreement agreement) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(agreement.getEngagedParty());
		references.add(agreement.getAssociatedAgreement());
		Optional.ofNullable(agreement.getAgreementSpecification()).map(List::of).ifPresent(references::add);
		return getCheckingMono(agreement, references)
				.onErrorMap(throwable -> new TmForumException(
						String.format("Was not able to create agreement %s",
								agreement.getId()),
						throwable,
						TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	@Override
	public Mono<HttpResponse<Object>> deleteAgreement(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<AgreementVO>>> listAgreement(@Nullable String fields, @Nullable Integer offset,
															   @Nullable Integer limit) {
		Mono<HttpResponse<List<AgreementVO>>> res = list(offset, limit, Agreement.TYPE_AGREEMENT, Agreement.class)
				.map(agStream -> agStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
		return res;
	}

	@Override
	public Mono<HttpResponse<AgreementVO>> patchAgreement(@NonNull String id,
														  @NonNull AgreementUpdateVO agreementUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such agreement cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		Agreement agreement = tmForumMapper.map(agreementUpdateVO, id);

		if (Optional.ofNullable(agreement.getAgreementItem()).map(List::isEmpty).orElse(false)) {
			agreement.setAgreementItem(null);
		}
		if (Optional.ofNullable(agreement.getEngagedParty()).map(List::isEmpty).orElse(false)) {
			agreement.setEngagedParty(null);
		}

		return patch(id, agreement, getCheckingMono(agreement), Agreement.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<AgreementVO>> retrieveAgreement(@NonNull String id, @Nullable String fields) {
		return retrieve(id, Agreement.class)
				.switchIfEmpty(
						Mono.error(
								new TmForumException("No such individual exists.",
										TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
