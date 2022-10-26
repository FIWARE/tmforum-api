package org.fiware.tmforum.party.repository;

import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.fiware.tmforum.common.repository.NgsiLdBaseRepository;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.JavaObjectMapper;
import org.fiware.tmforum.party.domain.TaxDefinition;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.fiware.tmforum.party.exception.PartyListException;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Repository implementation to serve as backend for the party-api
 */
@Singleton
public class PartyRepository extends NgsiLdBaseRepository {

    public PartyRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi, EntityVOMapper entityVOMapper, NGSIMapper ngsiMapper, JavaObjectMapper javaObjectMapper) {
        super(generalProperties, entitiesApi, javaObjectMapper, ngsiMapper, entityVOMapper);
    }

    public Mono<List<Organization>> findOrganizations(Integer offset, Integer limit) {
        return entitiesApi.queryEntities(generalProperties.getTenant(),
                        null,
                        null,
                        Organization.TYPE_ORGANIZATION,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        limit,
                        offset,
                        null,
                        getLinkHeader())
                .map(List::stream)
                .flatMap(entityVOStream -> zipToList(entityVOStream, Organization.class))
                .onErrorResume(t -> {
                    throw new PartyListException("Was not able to list parties.", t);
                });


    }

    public Mono<Organization> getOrganization(URI id) {
        return retrieveEntityById(id)
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, Organization.class));
    }

    public Mono<Individual> getIndividual(URI id) {
        return retrieveEntityById(id)
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, Individual.class));
    }


    public Mono<List<Individual>> findIndividuals(Integer offset, Integer limit) {
        return entitiesApi.queryEntities(generalProperties.getTenant(),
                        null,
                        null,
                        Individual.TYPE_INDIVIDUAL,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        limit,
                        offset,
                        null,
                        getLinkHeader())
                .map(List::stream)
                .flatMap(entityVOStream -> zipToList(entityVOStream, Individual.class))
                .onErrorResume(t -> {
                    throw new PartyListException("Was not able to list parties.", t);
                });
    }


    public Mono<TaxExemptionCertificate> updateTaxExemptionCertificate(TaxExemptionCertificate taxExemptionCertificate) {
        return patchEntity(taxExemptionCertificate.getId(), ngsiMapper.map(javaObjectMapper.toEntityVO(taxExemptionCertificate)))
                .then(getTaxExemptionCertificate(taxExemptionCertificate.getId()));
    }

    public Mono<TaxExemptionCertificate> getTaxExemptionCertificate(URI id) {
        return retrieveEntityById(id)
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, TaxExemptionCertificate.class));
    }

    public Mono<TaxDefinition> getTaxDefinition(URI id) {
        return retrieveEntityById(id)
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, TaxDefinition.class));
    }

    public Mono<TaxDefinition> createTaxDefinition(TaxDefinition taxDefinition) {
        return createEntity(javaObjectMapper.toEntityVO(taxDefinition), generalProperties.getTenant())
                .then(Mono.just(taxDefinition));
    }

    public Mono<TaxExemptionCertificate> createTaxExemptionCertificate(TaxExemptionCertificate taxExemptionCertificate) {

        List<TaxDefinition> taxDefinitions = Optional.ofNullable(taxExemptionCertificate.getTaxDefinition()).orElseGet(List::of);
        Mono<List<TaxDefinition>> taxDefSingle = Mono.zip(
                taxDefinitions.stream().map(this::createTaxDefinition).toList(),
                t -> Arrays.stream(t).map(TaxDefinition.class::cast).toList());

        return taxDefSingle
                .map(updatedTaxDefinitions -> {
                    taxExemptionCertificate.setTaxDefinition(updatedTaxDefinitions);
                    return taxExemptionCertificate;
                })
                .flatMap(cert -> createEntity(javaObjectMapper.toEntityVO(taxExemptionCertificate), generalProperties.getTenant()).then(Mono.just(cert)));
    }
}
