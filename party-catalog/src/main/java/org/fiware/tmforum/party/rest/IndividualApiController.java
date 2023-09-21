package org.fiware.tmforum.party.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.party.api.IndividualApi;
import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualUpdateVO;
import org.fiware.party.model.IndividualVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.party.TMForumMapper;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.domain.individual.IndividualIdentification;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class IndividualApiController extends AbstractPartyApiController<Individual> implements IndividualApi {

	private final TMForumMapper tmForumMapper;

	public IndividualApiController(ReferenceValidationService validationService, TmForumRepository partyRepository,
			TMForumMapper tmForumMapper, EventHandler eventHandler) {
		super(validationService, partyRepository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<IndividualVO>> createIndividual(@NonNull @Valid IndividualCreateVO individualCreateVO) {

		Individual individual = tmForumMapper.map(tmForumMapper.map(individualCreateVO,
				IdHelper.toNgsiLd(UUID.randomUUID().toString(), Individual.TYPE_INDIVIDUAL)));

		return create(getCheckingMono(individual), Individual.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Mono<Individual> getCheckingMono(Individual individual) {
		Optional.ofNullable(individual.getTaxExemptionCertificate()).ifPresent(this::validateTaxExemptions);
		Optional.ofNullable(individual.getIndividualIdentification())
				.ifPresent(this::validateIndividualIdentifications);

		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(individual.getRelatedParty());

		return getCheckingMono(individual, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create individual %s",
										individual.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}

	private void validateIndividualIdentifications(List<IndividualIdentification> individualIdentifications) {
		List<String> individualIds = individualIdentifications
				.stream()
				.map(IndividualIdentification::getIdentificationId)
				.toList();
		if (individualIds.size() != new HashSet<>(individualIds).size()) {
			throw new TmForumException(
					String.format("Duplicate individual identification ids are not allowed - ids: %s",
							individualIds), TmForumExceptionReason.INVALID_DATA);
		}
	}

	@Override
	public Mono<HttpResponse<Object>> deleteIndividual(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<IndividualVO>>> listIndividual(@Nullable String fields, @Nullable Integer offset,
			@Nullable Integer limit) {
		return list(offset, limit, Individual.TYPE_INDIVIDUAL, Individual.class)
				.map(individualStream -> individualStream.map(tmForumMapper::map).toList())
				.switchIfEmpty(Mono.just(List.of())).map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<IndividualVO>> patchIndividual(@NonNull String id,
			@NonNull IndividualUpdateVO individualUpdateVO) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such individual cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		Individual individual = tmForumMapper.map(individualUpdateVO, id);

		return patch(id, individual, getCheckingMono(individual), Individual.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<IndividualVO>> retrieveIndividual(@NonNull String id, @Nullable String fields) {

		return retrieve(id, Individual.class)
				.switchIfEmpty(
						Mono.error(
								new TmForumException("No such individual exists.", TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

}

