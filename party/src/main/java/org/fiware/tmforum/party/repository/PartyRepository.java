package org.fiware.tmforum.party.repository;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.fiware.ngsi.api.EntitiesApi;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.CommonTemplates;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.fiware.tmforum.common.repository.NgsiLdBaseRepository;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.JavaObjectMapper;
import org.fiware.tmforum.party.domain.TaxDefinition;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.domain.organization.Organization;

import javax.inject.Singleton;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Repository implementation to serve as backend for the party-api
 */
@Singleton
public class PartyRepository extends NgsiLdBaseRepository {

	private final EntityVOMapper entityVOMapper;
	private final NGSIMapper ngsiMapper;
	private final JavaObjectMapper javaObjectMapper;

	public PartyRepository(GeneralProperties generalProperties, EntitiesApi entitiesApi, EntityVOMapper entityVOMapper, NGSIMapper ngsiMapper, JavaObjectMapper javaObjectMapper) {
		super(generalProperties, entitiesApi);
		this.entityVOMapper = entityVOMapper;
		this.ngsiMapper = ngsiMapper;
		this.javaObjectMapper = javaObjectMapper;
	}

	public Completable createOrganization(Organization organization) {
		return createEntity(javaObjectMapper.toEntityVO(organization), generalProperties.getTenant());
	}

	public Completable deleteParty(URI id) {
		return entitiesApi.removeEntityById(id, generalProperties.getTenant(), null);
	}

	public Single<List<Organization>> findOrganizations() {
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
						null,
						null,
						getLinkHeader())
				.map(List::stream)
				.flatMap(entityVOStream -> zipToList(entityVOStream, Organization.class));

	}

	public Maybe<Organization> getOrganization(URI id) {
		return retrieveEntityById(id)
				.flatMapSingleElement(entityVO -> entityVOMapper.fromEntityVO(entityVO, Organization.class));
	}


	public Completable createIndividual(Individual individual) {
		return createEntity(javaObjectMapper.toEntityVO(individual), generalProperties.getTenant());
	}

	public Maybe<Individual> getIndividual(URI id) {
		return retrieveEntityById(id)
				.flatMapSingleElement(entityVO -> entityVOMapper.fromEntityVO(entityVO, Individual.class));
	}


	public Single<List<Individual>> findIndividuals() {
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
						null,
						null,
						getLinkHeader())
				.map(List::stream)
				.flatMap(entityVOStream -> zipToList(entityVOStream, Individual.class));

	}

	public Completable updateIndividual(String id, Individual individual) {
		return patchEntity(URI.create(id), ngsiMapper.map(javaObjectMapper.toEntityVO(individual)));
	}

	private <T> Single<List<T>> zipToList(Stream<EntityVO> entityVOStream, Class<T> targetClass) {
		return Single.zip(
				entityVOStream.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, targetClass)).toList(),
				oList -> Arrays.stream(oList).map(targetClass::cast).toList()
		);
	}

	public Single<TaxExemptionCertificate> updateTaxExemptionCertificate(TaxExemptionCertificate taxExemptionCertificate) {
		return patchEntity(taxExemptionCertificate.getId(), ngsiMapper.map(javaObjectMapper.toEntityVO(taxExemptionCertificate)))
				.andThen(getTaxExemptionCertificate(taxExemptionCertificate.getId()).toSingle());
	}

	public Maybe<TaxExemptionCertificate> getTaxExemptionCertificate(URI id) {
		return retrieveEntityById(id)
				.flatMapSingleElement(entityVO -> entityVOMapper.fromEntityVO(entityVO, TaxExemptionCertificate.class));
	}

	public Maybe<TaxDefinition> getTaxDefinition(URI id) {
		return retrieveEntityById(id)
				.flatMapSingleElement(entityVO -> entityVOMapper.fromEntityVO(entityVO, TaxDefinition.class));
	}

	public Single<TaxDefinition> createTaxDefinition(TaxDefinition taxDefinition) {

		return createEntity(javaObjectMapper.toEntityVO(taxDefinition), generalProperties.getTenant())
				.toSingleDefault(taxDefinition);
	}

	public Single<TaxExemptionCertificate> createTaxExemptionCertificate(TaxExemptionCertificate taxExemptionCertificate) {

		List<TaxDefinition> taxDefinitions = Optional.ofNullable(taxExemptionCertificate.getTaxDefinition()).orElseGet(List::of);
		Single<List<TaxDefinition>> taxDefSingle = Single.zip(
				taxDefinitions.stream().map(this::createTaxDefinition).toList(),
				t -> Arrays.stream(t).map(TaxDefinition.class::cast).toList());

		return taxDefSingle
				.map(updatedTaxDefinitions -> {
					taxExemptionCertificate.setTaxDefinition(updatedTaxDefinitions);
					return taxExemptionCertificate;
				})
				.flatMap(cert -> createEntity(javaObjectMapper.toEntityVO(taxExemptionCertificate), generalProperties.getTenant()).toSingleDefault(cert));
	}
}
