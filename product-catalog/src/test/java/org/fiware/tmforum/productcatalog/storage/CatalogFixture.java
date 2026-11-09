package org.fiware.tmforum.productcatalog.storage;

import io.github.wistefan.mapping.UnmappedProperty;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.storage.contract.UpdateFixture;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Catalogs for the port contract: {@code name} is mutated, {@code description} stays untouched and
 * {@code relatedParty} is the list, which reads back from its properties, so no referenced party has to exist.
 */
class CatalogFixture implements UpdateFixture<Catalog> {

	private static final String NAME = "contract-catalog";
	private static final String MUTATED_NAME = "mutated-contract-catalog";
	private static final String DESCRIPTION = "untouched description";
	private static final String EXTENSION_NAME = "extension-named-like-name";
	private static final String NAME_ATTRIBUTE = "name";
	private static final String PARTY_ID_FORMAT = "urn:ngsi-ld:individual:%s";

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
	public Catalog withExtensionNamedLikeTheMutableAttribute(URI id) {
		Catalog catalog = newEntity(id);
		catalog.addAdditionalProperties(NAME_ATTRIBUTE, EXTENSION_NAME);
		return catalog;
	}

	@Override
	public Set<String> extensionNames(Catalog entity) {
		return Optional.ofNullable(entity.getAdditionalProperties()).orElse(List.of()).stream()
				.map(UnmappedProperty::getName)
				.collect(Collectors.toSet());
	}

	private static RelatedParty relatedParty() {
		return new RelatedParty(String.format(PARTY_ID_FORMAT, UUID.randomUUID()));
	}
}
