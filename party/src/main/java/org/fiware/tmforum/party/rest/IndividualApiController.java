package org.fiware.tmforum.party.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.extern.slf4j.Slf4j;
import org.fiware.party.api.IndividualApi;
import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualUpdateVO;
import org.fiware.party.model.IndividualVO;
import org.fiware.tmforum.common.exception.DeletionException;
import org.fiware.tmforum.common.exception.DeletionExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.party.TMForumMapper;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.exception.PartyCreationException;
import org.fiware.tmforum.party.exception.PartyExceptionReason;
import org.fiware.tmforum.party.exception.PartyRetrievalException;
import org.fiware.tmforum.party.exception.PartyUpdateException;
import org.fiware.tmforum.party.repository.PartyRepository;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Slf4j
@Controller("${general.basepath:/}")
public class IndividualApiController extends AbstractApiController implements IndividualApi {


    public IndividualApiController(TMForumMapper tmForumMapper, PartyRepository partyRepository, ReferenceValidationService validationService) {
        super(tmForumMapper, partyRepository, validationService);
    }

    @Override
    public Mono<HttpResponse<IndividualVO>> createIndividual(@Valid IndividualCreateVO individualCreateVO) {
        IndividualVO individualVO = tmForumMapper.map(individualCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Individual.TYPE_INDIVIDUAL));
        Individual individual = tmForumMapper.map(individualVO);
        Mono<Individual> individualMono = getCheckingMono(individual);

        individualMono = taxExemptionHandlingMono(
                individual,
                individualMono,
                individual.getTaxExemptionCertificate(),
                individual::setTaxExemptionCertificate,
                false);

        return individualMono
                .flatMap(individualToCreate -> partyRepository.createDomainEntity(individualToCreate).then(Mono.just(individualToCreate)))
                .onErrorMap(t -> {
                    if (t instanceof HttpClientResponseException e) {
                        return switch (e.getStatus()) {
                            case CONFLICT -> new PartyCreationException(String.format("Conflict on creating the individual: %s", e.getMessage()), PartyExceptionReason.CONFLICT);
                            case BAD_REQUEST -> new PartyCreationException(String.format("Did not receive a valid individual: %s.", e.getMessage()), PartyExceptionReason.INVALID_DATA);
                            default -> new PartyCreationException(String.format("Unspecified downstream error: %s", e.getMessage()), PartyExceptionReason.UNKNOWN);
                        };
                    } else {
                        return t;
                    }
                })
                .cast(Individual.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }


    private Mono<Individual> getCheckingMono(Individual individual) {
        Mono<Individual> individualMono = Mono.just(individual);

        if (individual.getRelatedParty() != null && !individual.getRelatedParty().isEmpty()) {
            Mono<Individual> checkingMono;
            checkingMono = validationService.getCheckingMono(individual.getRelatedParty(), individual)
                    .onErrorMap(throwable -> new PartyCreationException(String.format("Was not able to create individual %s", individual.getId()), throwable, PartyExceptionReason.INVALID_RELATIONSHIP));
            individualMono = Mono.zip(individualMono, checkingMono, (p1, p2) -> p1);
        }

        return individualMono;
    }


    @Override
    public Mono<HttpResponse<Object>> deleteIndividual(String id) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new DeletionException("Did not receive a valid id, such individual cannot exist.", DeletionExceptionReason.NOT_FOUND);
        }
        return partyRepository.deleteDomainEntity(URI.create(id))
                .then(Mono.just(HttpResponse.noContent()));
    }

    @Override
    public Mono<HttpResponse<List<IndividualVO>>> listIndividual(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        offset = Optional.ofNullable(offset).orElse(DEFAULT_OFFSET);
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);

        return partyRepository
                .findIndividuals(offset, limit)
                .map(List::stream)
                .map(organizationStream -> organizationStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<IndividualVO>> patchIndividual(String id, IndividualUpdateVO individualUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new PartyUpdateException("Did not receive a valid id, such individual cannot exist.", PartyExceptionReason.NOT_FOUND);
        }

        Individual updatedIndividual = tmForumMapper.map(tmForumMapper.map(individualUpdateVO, id));

        URI idUri = URI.create(id);
        return partyRepository
                .getIndividual(idUri)
                .flatMap(individual ->
                        taxExemptionHandlingMono(
                                individual,
                                getCheckingMono(updatedIndividual),
                                updatedIndividual.getTaxExemptionCertificate(),
                                individual::setTaxExemptionCertificate,
                                true)
                                .flatMap(ui -> partyRepository
                                        .updateDomainEntity(id, updatedIndividual)
                                        .then(partyRepository.getIndividual(idUri))
                                        .map(tmForumMapper::map)
                                        .map(HttpResponse::ok)
                                        .onErrorMap(error -> new PartyUpdateException("Was not able to update individual.", PartyExceptionReason.UNKNOWN)))
                )
                .map(HttpResponse.class::cast);
    }

    @Override
    public Mono<HttpResponse<IndividualVO>> retrieveIndividual(String id, @Nullable String fields) {

        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new PartyRetrievalException("Did not receive a valid id, such individual cannot exist.", PartyExceptionReason.NOT_FOUND);
        }

        return partyRepository
                .getIndividual(URI.create(id))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok)
                .switchIfEmpty(Mono.error(new PartyRetrievalException("No such individual exists.", PartyExceptionReason.NOT_FOUND)))
                .map(HttpResponse.class::cast);

    }
}

