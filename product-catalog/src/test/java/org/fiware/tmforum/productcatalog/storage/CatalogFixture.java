package org.fiware.tmforum.productcatalog.storage;

import io.github.wistefan.mapping.UnmappedProperty;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.storage.contract.UpdateFixture;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Catalogs for the port contract: {@code name} is mutated, {@code description} stays untouched and
 * {@code relatedParty} is the list, which reads back from its properties, so no referenced party has to exist, and
 * {@code validFor} is the object.
 */
class CatalogFixture implements UpdateFixture<Catalog> {

	private static final String NAME = "contract-catalog";
	private static final String MUTATED_NAME = "mutated-contract-catalog";
	private static final String DESCRIPTION = "untouched description";
	private static final String EXTENSION_VALUE = "extension-named-like-name";
	private static final String NAME_ATTRIBUTE = "name";
	private static final String PARTY_ID_FORMAT = "urn:ngsi-ld:individual:%s";
	private static final Instant VALID_FROM = Instant.parse("2026-01-01T00:00:00Z");

	@Override
	public Class<Catalog> entityClass() {
		return Catalog.class;
	}

	@Override
	public String storedType() {
		return Catalog.TYPE_CATALOG;
	}

	@Override
	public Catalog newEntity(URI id) {
		Catalog catalog = new Catalog(id.toString());
		catalog.setName(NAME);
		catalog.setDescription(DESCRIPTION);
		catalog.setRelatedParty(List.of(relatedParty(), relatedParty()));
		TimePeriod validFor = new TimePeriod();
		validFor.setStartDateTime(VALID_FROM);
		catalog.setValidFor(validFor);
		return catalog;
	}

	@Override
	public URI id(Catalog entity) {
		return entity.getId();
	}

	@Override
	public Catalog mutate(URI id) {
		Catalog catalog = new Catalog(id.toString());
		catalog.setName(MUTATED_NAME);
		return catalog;
	}

	@Override
	public Catalog emptiedList(URI id) {
		Catalog catalog = new Catalog(id.toString());
		catalog.setRelatedParty(List.of());
		return catalog;
	}

	@Override
	public Object mutableValue(Catalog entity) {
		return entity.getName();
	}

	@Override
	public Object untouchedValue(Catalog entity) {
		return entity.getDescription();
	}

	@Override
	public List<?> listValue(Catalog entity) {
		return entity.getRelatedParty();
	}

	@Override
	public Catalog withExtensionNamedLikeTheMutableAttribute(Catalog catalog) {
		catalog.addAdditionalProperties(NAME_ATTRIBUTE, EXTENSION_VALUE);
		return catalog;
	}

	@Override
	public Object extensionValue() {
		return EXTENSION_VALUE;
	}

	@Override
	public Set<String> extensionNames(Catalog entity) {
		return Optional.ofNullable(entity.getAdditionalProperties()).orElse(List.of()).stream()
				.map(UnmappedProperty::getName)
				.collect(Collectors.toSet());
	}

	@Override
	public Catalog withExtension(Catalog catalog, String name, Object value) {
		catalog.addAdditionalProperties(name, value);
		return catalog;
	}

	@Override
	public Object extensionValue(Catalog entity, String name) {
		return Optional.ofNullable(entity.getAdditionalProperties()).orElse(List.of()).stream()
				.filter(property -> property.getName().equals(name))
				.map(UnmappedProperty::getValue)
				.findFirst()
				.orElse(null);
	}

	@Override
	public Catalog emptiedObject(URI id) {
		Catalog catalog = new Catalog(id.toString());
		catalog.setValidFor(new TimePeriod());
		return catalog;
	}

	@Override
	public Object objectValue(Catalog entity) {
		return entity.getValidFor();
	}

	private static RelatedParty relatedParty() {
		return new RelatedParty(String.format(PARTY_ID_FORMAT, UUID.randomUUID()));
	}
}
