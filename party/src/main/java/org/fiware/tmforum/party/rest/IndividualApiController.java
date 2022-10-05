package org.fiware.tmforum.party.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.party.api.IndividualApi;
import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualUpdateVO;
import org.fiware.party.model.IndividualVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.party.TMForumMapper;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.exception.PartyCreationException;
import org.fiware.tmforum.party.repository.PartyRepository;
import reactor.core.publisher.Mono;

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
    public Mono<HttpResponse<IndividualVO>> createIndividual(@Valid IndividualCreateVO individualCreateVO) {
        IndividualVO individualVO = tmForumMapper.map(individualCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Individual.TYPE_INDIVIDUAL));
        Individual individual = tmForumMapper.map(individualVO);
        Mono<Individual> individualMono = getCheckingSingle(individual);

        individualMono = taxExemptionHandlingMono(individual, individualMono, false);

        return individualMono
                .flatMap(individualToCreate -> partyRepository.createIndividual(individualToCreate).then(Mono.just(individualToCreate)))
                .cast(Individual.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<Individual> taxExemptionHandlingMono(Individual individual, Mono<Individual> individualSingle, boolean update) {
        List<TaxExemptionCertificate> taxExemptionCertificates = Optional.ofNullable(individual.getTaxExemptionCertificate()).orElseGet(List::of);
        if (!taxExemptionCertificates.isEmpty()) {
            Mono<List<TaxExemptionCertificate>> taxExemptionCertificatesSingles = Mono.zip(
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

            Mono<Individual> updatingSingle = taxExemptionCertificatesSingles
                    .map(updatedTaxExemptions -> {
                        individual.setTaxExemptionCertificate(updatedTaxExemptions);
                        return individual;
                    });
            individualSingle = Mono.zip(individualSingle, updatingSingle, (individual1, individual2) -> individual1);
        }
        return individualSingle;
    }

    private Mono<Individual> getCheckingSingle(Individual individual) {
        Mono<Individual> individualMono = Mono.just(individual);

        if (individual.getRelatedParty() != null && !individual.getRelatedParty().isEmpty()) {
            Mono<Individual> checkingMono;
            checkingMono = validationService.getCheckingMono(individual.getRelatedParty(), individual)
                    .onErrorMap(throwable -> new PartyCreationException(String.format("Was not able to create individual %s", individual.getId()), throwable));
            individualMono = Mono.zip(individualMono, checkingMono, (p1, p2) -> p1);
        }

        return individualMono;
    }


    @Override
    public Mono<HttpResponse<Object>> deleteIndividual(String id) {
        return partyRepository
                .deleteParty(IdHelper.toNgsiLd(id, Individual.TYPE_INDIVIDUAL))
                .then(Mono.just(HttpResponse.noContent()));
    }

    @Override
    public Mono<HttpResponse<List<IndividualVO>>> listIndividual(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return partyRepository
                .findIndividuals()
                .map(List::stream)
                .map(organizationStream -> organizationStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<IndividualVO>> patchIndividual(String id, IndividualUpdateVO individual) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            return Mono.just(HttpResponse.notFound());
        }

        Individual updatedIndividual = tmForumMapper.map(tmForumMapper.map(individual, id));

        URI idUri = URI.create(id);
        return partyRepository
                .getIndividual(idUri)
                .flatMap(inidvidual ->
                        taxExemptionHandlingMono(updatedIndividual, getCheckingSingle(updatedIndividual), true)
                                .flatMap(ui -> partyRepository
                                        .updateIndividual(id, updatedIndividual)
                                        .then(partyRepository.getIndividual(idUri))
                                        .map(tmForumMapper::map)
                                        .map(HttpResponse::ok)
                                        .onErrorResume(error -> Mono.just(HttpResponse.status(HttpStatus.BAD_GATEWAY))))
                )
                .switchIfEmpty(Mono.just(HttpResponse.notFound()))
                .map(HttpResponse.class::cast);
    }

    @Override
    public Mono<HttpResponse<IndividualVO>> retrieveIndividual(String id, @Nullable String fields) {

        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            return Mono.just(HttpResponse.notFound());
        }

        return partyRepository
                .getIndividual(URI.create(id))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok)
                .switchIfEmpty(Mono.just(HttpResponse.notFound()))
                .map(HttpResponse.class::cast);

    }
}

