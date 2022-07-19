package org.fiware.tmforum.party.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.party.api.IndividualApi;
import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualUpdateVO;
import org.fiware.party.model.IndividualVO;
import org.fiware.tmforum.common.ValidationService;
import org.fiware.tmforum.party.TMForumMapper;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.repository.PartyRepository;

import javax.annotation.Nullable;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IndividualApiController implements IndividualApi {

	private final TMForumMapper tmForumMapper;
	private final PartyRepository partyRepository;
	private final ValidationService validationService;

	@Override
	public Single<HttpResponse<IndividualVO>> createIndividual(@Valid IndividualCreateVO individualCreateVO) {
		IndividualVO individualVO = tmForumMapper.map(individualCreateVO);
		Individual individual = tmForumMapper.map(individualVO);

		Single<Individual> individualSingle = Single.just(individual);

		if (individual.getRelatedParty() != null && !individual.getRelatedParty().isEmpty()) {
			Single<Individual> checkingSingle = validationService.getCheckingSingle(individual.getRelatedParty(), individual);
			individualSingle = Single.zip(individualSingle, checkingSingle, (p1, p2) -> p1);
		}

		List<TaxExemptionCertificate> taxExemptionCertificates = individual.getTaxExemptionCertificate();
		if (taxExemptionCertificates != null && !taxExemptionCertificates.isEmpty()) {
			Single<List<TaxExemptionCertificate>> taxExemptionCertificatesSingles = Single.zip(taxExemptionCertificates.stream().map(partyRepository::getOrCreate).toList(), t -> Arrays.stream(t).map(TaxExemptionCertificate.class::cast).toList());
			Single<Individual> updatingSingle = taxExemptionCertificatesSingles
					.map(updatedTaxExemptions -> {
						individual.setTaxExemptionCertificate(updatedTaxExemptions);
						return individual;
					});
			individualSingle = Single.zip(individualSingle, updatingSingle, (individual1, individual2) -> individual1);
		}

		return individualSingle
				.flatMap(individualToCreate -> partyRepository.createIndividual(individualToCreate).toSingleDefault(individualToCreate))
				.cast(Individual.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}


	@Override
	public Single<HttpResponse<Object>> deleteIndividual(String id) {
		return partyRepository.deleteParty(id).toSingleDefault(HttpResponse.noContent());
	}

	@Override
	public Single<HttpResponse<List<IndividualVO>>> listIndividual(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
		return partyRepository.findIndividuals()
				.map(List::stream).map(organizationStream -> organizationStream.map(tmForumMapper::map).toList())
				.map(HttpResponse::ok);
	}

	@Override
	public Single<HttpResponse<IndividualVO>> patchIndividual(String id, IndividualUpdateVO individual) {
		// implement proper patch
		return null;
	}

	@Override
	public Single<HttpResponse<IndividualVO>> retrieveIndividual(String id, @Nullable String fields) {
		return partyRepository
				.getIndividual(id)
				.map(tmForumMapper::map)
				.toSingle()
				.map(HttpResponse::ok);
	}
}

