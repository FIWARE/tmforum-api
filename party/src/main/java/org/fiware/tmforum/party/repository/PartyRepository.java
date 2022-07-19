package org.fiware.tmforum.party.repository;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.fiware.ngsi.api.EntitiesApi;
import org.fiware.tmforum.common.CommonTemplates;
import org.fiware.tmforum.common.configuration.GeneralProperties;
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

/**
 * Repository implementation to serve as backend for the party-api
 */
@Singleton
public class PartyRepository extends NgsiLdBaseRepository {

	private final EntityVOMapper entityVOMapper;
	private final JavaObjectMapper javaObjectMapper;

	public PartyRepository(GeneralProperties generalProperties, EntitiesApi entitiesApi, EntityVOMapper entityVOMapper, JavaObjectMapper javaObjectMapper) {
		super(generalProperties, entitiesApi);
		this.entityVOMapper = entityVOMapper;
		this.javaObjectMapper = javaObjectMapper;
	}

	public Completable createOrganization(Organization organization) {
		return createEntity(javaObjectMapper.toEntityVO(organization), generalProperties.getTenant());
	}

	public Completable deleteParty(String id) {
		return entitiesApi.removeEntityById(URI.create(id), generalProperties.getTenant(), null);
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
				.map(entityVOStream -> entityVOStream.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, Organization.class)).toList());

	}

	public Maybe<Organization> getOrganization(String id) {
		return retrieveEntityById(URI.create(id))
				.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, Organization.class));
	}

	public Completable createIndividual(Individual individual) {
		return createEntity(javaObjectMapper.toEntityVO(individual), generalProperties.getTenant());
	}

	public Maybe<Individual> getIndividual(String id) {
		return retrieveEntityById(URI.create(id))
				.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, Individual.class));
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
				.map(entityVOStream -> entityVOStream.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, Individual.class)).toList());

	}

	public Completable createTaxExemptionCertificate(TaxExemptionCertificate taxExemptionCertificate) {
		return createEntity(javaObjectMapper.toEntityVO(taxExemptionCertificate), generalProperties.getTenant());
	}

	public Maybe<TaxExemptionCertificate> getTaxExemptionCertificate(String id) {
		return retrieveEntityById(URI.create(id))
				.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, TaxExemptionCertificate.class));
	}


	public Single<List<TaxExemptionCertificate>> findTaxExemptionCertificates() {
		return entitiesApi.queryEntities(generalProperties.getTenant(),
						null,
						null,
						TaxExemptionCertificate.TYPE_TAX_EXEMPTION_CERTIFICATE,
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
				.map(entityVOStream -> entityVOStream.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, TaxExemptionCertificate.class)).toList());

	}

	public Completable createTaxDefinition(TaxDefinition taxDefinition) {
		return createEntity(javaObjectMapper.toEntityVO(taxDefinition), generalProperties.getTenant());
	}

	public Maybe<TaxDefinition> getTaxDefinition(String id) {
		return retrieveEntityById(URI.create(id))
				.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, TaxDefinition.class));
	}


	public Single<List<TaxDefinition>> findTaxDefinitions() {
		return entitiesApi.queryEntities(generalProperties.getTenant(),
						null,
						null,
						TaxDefinition.TYPE_TAX_DEFINITION,
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
				.map(entityVOStream -> entityVOStream.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, TaxDefinition.class)).toList());

	}

	public Single<TaxDefinition> getOrCreate(TaxDefinition taxDefinition) {
		Optional<URI> optionalID = Optional.ofNullable(taxDefinition.getId());
		if (optionalID.isPresent()) {
			return getTaxDefinition(optionalID.get().toString())
					.toSingle();

		} else {
			URI specID = URI.create(String.format(CommonTemplates.ID_TEMPLATE, TaxDefinition.TYPE_TAX_DEFINITION, UUID.randomUUID()));
			taxDefinition.setId(specID);
			return createTaxDefinition(taxDefinition).toSingleDefault(taxDefinition);
		}
	}

	public Single<TaxExemptionCertificate> getOrCreate(TaxExemptionCertificate taxExemptionCertificate) {
		Optional<URI> optionalCertID = Optional.ofNullable(taxExemptionCertificate.getId());
		if (optionalCertID.isPresent()) {
			return getTaxExemptionCertificate(optionalCertID.get().toString())
					.toSingle();
		} else {
			URI specID = URI.create(String.format(CommonTemplates.ID_TEMPLATE, TaxExemptionCertificate.TYPE_TAX_EXEMPTION_CERTIFICATE, UUID.randomUUID()));
			taxExemptionCertificate.setId(specID);

			List<TaxDefinition> taxDefinitions = Optional.ofNullable(taxExemptionCertificate.getTaxDefinition()).orElseGet(List::of);
			Single<List<TaxDefinition>> taxDefSingle = Single.zip(taxDefinitions.stream().map(this::getOrCreate).toList(), t -> Arrays.stream(t).map(TaxDefinition.class::cast).toList());

			return taxDefSingle
					.map(updatedTaxDefinitions -> {
						taxExemptionCertificate.setTaxDefinition(updatedTaxDefinitions);
						return taxExemptionCertificate;
					})
					.flatMap(cert -> createTaxExemptionCertificate(cert).toSingleDefault(cert));
		}
	}
}
