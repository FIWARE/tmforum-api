package org.fiware.tmforum.party.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.party.api.IndividualApi;
import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualUpdateVO;
import org.fiware.party.model.IndividualVO;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.party.TMForumMapper;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.exception.PartyCreationException;
import org.fiware.tmforum.party.repository.PartyRepository;

import javax.annotation.Nullable;
import javax.validation.Valid;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
@RequiredArgsConstructor
public class IndividualApiController implements IndividualApi {

	private final TMForumMapper tmForumMapper;
	private final PartyRepository partyRepository;
	private final ReferenceValidationService validationService;

	@Override
	public Single<HttpResponse<IndividualVO>> createIndividual(@Valid IndividualCreateVO individualCreateVO) {
		IndividualVO individualVO = tmForumMapper.map(individualCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Individual.TYPE_INDIVIDUAL));
		Individual individual = tmForumMapper.map(individualVO);
		Single<Individual> individualSingle = getCheckingSingle(individual);

		individualSingle = taxExemptionHandlingSingle(individual, individualSingle, false);

		return individualSingle
				.flatMap(individualToCreate -> partyRepository.createIndividual(individualToCreate).toSingleDefault(individualToCreate))
				.cast(Individual.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	private Single<Individual> taxExemptionHandlingSingle(Individual individual, Single<Individual> individualSingle, boolean update) {
		List<TaxExemptionCertificate> taxExemptionCertificates = Optional.ofNullable(individual.getTaxExemptionCertificate()).orElseGet(List::of);
		if (!taxExemptionCertificates.isEmpty()) {
			Single<List<TaxExemptionCertificate>> taxExemptionCertificatesSingles = Single.zip(
					taxExemptionCertificates
							.stream()
							.map(teCert -> {
										if (update) {
											return partyRepository.updateTaxExemptionCertificate(teCert);
										} else {
											return partyRepository.createTaxExemptionCertificate(teCert);
										}
									}
							)
							.toList(),
					t -> Arrays.stream(t).map(TaxExemptionCertificate.class::cast).toList());

			Single<Individual> updatingSingle = taxExemptionCertificatesSingles
					.map(updatedTaxExemptions -> {
						individual.setTaxExemptionCertificate(updatedTaxExemptions);
						return individual;
					});
			individualSingle = Single.zip(individualSingle, updatingSingle, (individual1, individual2) -> individual1);
		}
		return individualSingle;
	}

	private Single<Individual> getCheckingSingle(Individual individual) {
		Single<Individual> individualSingle = Single.just(individual);

		if (individual.getRelatedParty() != null && !individual.getRelatedParty().isEmpty()) {
			Single<Individual> checkingSingle;
			try {
				checkingSingle = validationService.getCheckingSingleOrThrow(individual.getRelatedParty(), individual);
			} catch (NonExistentReferenceException e) {
				throw new PartyCreationException(String.format("Was not able to create individual %s", individual.getId()), e);
			}
			individualSingle = Single.zip(individualSingle, checkingSingle, (p1, p2) -> p1);
		}
		return individualSingle;
	}


	@Override
	public Single<HttpResponse<Object>> deleteIndividual(String id) {
		return partyRepository
				.deleteParty(IdHelper.toNgsiLd(id, Individual.TYPE_INDIVIDUAL))
				.toSingleDefault(HttpResponse.noContent());
	}

	@Override
	public Single<HttpResponse<List<IndividualVO>>> listIndividual(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
		return partyRepository
				.findIndividuals()
				.map(List::stream)
				.map(organizationStream -> organizationStream.map(tmForumMapper::map).toList())
				.map(HttpResponse::ok);
	}

	@Override
	public Single<HttpResponse<IndividualVO>> patchIndividual(String id, IndividualUpdateVO individual) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			return Single.just(HttpResponse.notFound());
		}

		Individual updatedIndividual = tmForumMapper.map(tmForumMapper.map(individual, id));

		URI idUri = URI.create(id);
		return partyRepository
				.getIndividual(idUri)
				.isEmpty()
				.flatMap(isEmpty -> {
					if (isEmpty) {
						return Single.just(HttpResponse.notFound());
					} else {
						return taxExemptionHandlingSingle(updatedIndividual, getCheckingSingle(updatedIndividual), true)
								.flatMap(ui -> partyRepository
										.updateIndividual(id, updatedIndividual)
										.andThen(partyRepository.getIndividual(idUri))
										.map(tmForumMapper::map)
										.map(HttpResponse::ok)
										.toSingle(HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR))
										.onErrorReturn(error -> HttpResponse.status(HttpStatus.BAD_GATEWAY)));
					}
				});
	}

	@Override
	public Single<HttpResponse<IndividualVO>> retrieveIndividual(String id, @Nullable String fields) {

		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			return Single.just(HttpResponse.notFound());
		}

		return partyRepository
				.getIndividual(URI.create(id))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok)
				.switchIfEmpty(Single.just(HttpResponse.notFound()))
				.map(HttpResponse.class::cast);

	}
}

