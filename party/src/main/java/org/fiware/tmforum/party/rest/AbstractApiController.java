package org.fiware.tmforum.party.rest;

import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.party.TMForumMapper;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.party.repository.PartyRepository;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class AbstractApiController {

    protected final TMForumMapper tmForumMapper;
    protected final PartyRepository partyRepository;
    protected final ReferenceValidationService validationService;


    protected <T> Mono<T> taxExemptionHandlingMono(T party, Mono<T> partyMono, List<TaxExemptionCertificate> taxExemptionCertificateList, Consumer<List<TaxExemptionCertificate>> partyUpdater, boolean update) {
        List<TaxExemptionCertificate> taxExemptionCertificates = Optional.ofNullable(taxExemptionCertificateList).orElseGet(List::of);
        if (!taxExemptionCertificates.isEmpty()) {
            Mono<List<TaxExemptionCertificate>> taxExemptionCertificatesSingles = Mono.zip(
                    taxExemptionCertificates
                            .stream()
                            .map(teCert -> {
                                if (update) {
                                    return partyRepository.updateTaxExemptionCertificate(teCert)
                                            .onErrorResume(t -> partyRepository.createTaxExemptionCertificate(teCert));
                                } else {
                                    return partyRepository.createTaxExemptionCertificate(teCert);
                                }
                            })
                            .toList(),
                    t -> Arrays.stream(t).map(TaxExemptionCertificate.class::cast).toList());

            Mono<T> updatingMono = taxExemptionCertificatesSingles
                    .map(updatedTaxExemptions -> {
                        partyUpdater.accept(updatedTaxExemptions);
                        return party;
                    });
            partyMono = Mono.zip(partyMono, updatingMono, (party1, party2) -> party1);
        }
        return partyMono;
    }

}
