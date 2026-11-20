package org.fiware.tmforum.migration.postgresql;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.NgsiLdWriteOperation;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.GeoPropertyVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.fiware.tmforum.common.repository.NgsiLdEntityRepository;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.DatasetIdRewrite;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.Entry;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.FillableField;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillReport.AttributeResult;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.postgresql.DocumentMapper;
import org.fiware.tmforum.storage.postgresql.DocumentMode;
import org.fiware.tmforum.storage.postgresql.PostgresDocumentMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Fills absent fields of the catalog's references in the broker, for deployments that stay on NGSI-LD, and rewrites
 * the datasetIds of {@link ReferenceBackfillCatalog#datasetIdRewrites()}. Entities are read page by page and turned
 * into the PostgreSQL document form, so {@link ReferenceFiller} applies the rule of the PostgreSQL backfill to the same
 * keys. Only changed attributes are written, through the NGSI-LD adapter's {@code updateDomainEntity}.
 * <p>
 * Without {@code replaceOnUpdate} the broker appends, which replaces stored instances by datasetId. An attribute whose
 * stored datasetIds differ from the written ones (the old {@code productOrderItem} format) is therefore replaced as a
 * whole: on Orion-LD it is deleted first and restored from its read value if the write then fails; on Scorpio, whose
 * attribute delete is broken in 5.0.3, Update Attributes replaces all its instances. Each write is a normal broker
 * update: it moves {@code modifiedAt} and notifies the broker's own subscriptions. Not transactional; a re-run is a
 * no-op.
 */
@Slf4j
class BrokerReferenceBackfill {

	/** Ids per target query; keeps the request URL short. */
	static final int TARGET_BATCH_SIZE = 50;
	static final String DATASET_ID_REWRITE_SUFFIX = " (datasetId)";
	private static final String TYPE_KEY = "type";
	private static final String DEFAULT_INSTANCE = "";

	private final BrokerSource source;
	private final NgsiLdEntityRepository repository;
	private final JavaObjectMapper javaObjectMapper;
	private final DocumentMapper documents;
	private final boolean replaceOnUpdate;
	private final boolean replaceByUpdateAttributes;
	private final NGSIMapper ngsiMapper;
	private final EntityTypeRegistry registry;
	private final int pageSize;
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final List<Attribute> attributes;

	BrokerReferenceBackfill(ApplicationContext sourceContext, ReferenceBackfillCatalog catalog,
			EntityTypeRegistry registry, int pageSize) {
		this.source = new BrokerSource(sourceContext);
		this.repository = sourceContext.getBean(NgsiLdEntityRepository.class);
		this.javaObjectMapper = sourceContext.getBean(JavaObjectMapper.class);
		// the PostgreSQL document form only, so the catalog keys apply; nothing is written to a database
		this.documents = new PostgresDocumentMapper(sourceContext.getBean(ObjectMapper.class)).storage();
		this.replaceOnUpdate = sourceContext.getBean(GeneralProperties.class).isReplaceOnUpdate();
		this.replaceByUpdateAttributes = sourceContext.getEnvironment().getActiveNames()
				.contains(BrokerType.SCORPIO.getEnvironment());
		this.ngsiMapper = sourceContext.getBean(NGSIMapper.class);
		this.registry = registry;
		this.pageSize = pageSize;
		this.attributes = attributes(catalog);
	}

	/**
	 * One catalog attribute and its running counts; {@code fields} is empty for a datasetId rewrite.
	 */
	private static final class Attribute {
		private final String label;
		private final List<String> ownerTypes;
		private final String name;
		private final List<FillableField> fields;
		private final boolean rewrite;
		private long filledReferences;
		private long missingTargets;
		private long changedEntities;

		private Attribute(String label, List<String> ownerTypes, String name, List<FillableField> fields,
				boolean rewrite) {
			this.label = label;
			this.ownerTypes = ownerTypes;
			this.name = name;
			this.fields = fields;
			this.rewrite = rewrite;
		}

		private AttributeResult result() {
			return new AttributeResult(label, filledReferences, missingTargets, changedEntities);
		}
	}

	/**
	 * An owner entity as read, its document, and the attributes to write with their new document values.
	 */
	private record Owner(EntityVO entity, ObjectNode document, Map<Attribute, JsonNode> changes) {
	}

	private static List<Attribute> attributes(ReferenceBackfillCatalog catalog) {
		List<Attribute> attributes = new ArrayList<>();
		for (Entry entry : catalog.entries()) {
			// nothing to fill, the PostgreSQL backfill skips them too
			if (!entry.fillableFields().isEmpty()) {
				attributes.add(new Attribute(PostgresReferenceBackfill.label(entry), entry.ownerTypes(),
						entry.attribute(), entry.fillableFields(), false));
			}
		}
		for (DatasetIdRewrite rewrite : catalog.datasetIdRewrites()) {
			attributes.add(new Attribute(
					PostgresReferenceBackfill.label(rewrite.ownerTypes(), rewrite.attribute()) + DATASET_ID_REWRITE_SUFFIX,
					rewrite.ownerTypes(), rewrite.attribute(), List.of(), true));
		}
		return attributes;
	}

	/**
	 * Runs the backfill, or on a dry run only counts. Never throws: every failure is an error of the report.
	 */
	ReferenceBackfillReport run(boolean dryRun) {
		ReferenceBackfillReport report = new ReferenceBackfillReport(dryRun);
		Map<String, List<Attribute>> attributesByType = new TreeMap<>();
		attributes.forEach(attribute -> attribute.ownerTypes.forEach(type ->
				attributesByType.computeIfAbsent(type, t -> new ArrayList<>()).add(attribute)));
		attributesByType.forEach((type, typeAttributes) -> {
			try {
				backfillType(type, typeAttributes, dryRun, report);
			} catch (RuntimeException e) {
				log.warn("Was not able to backfill {}.", type, e);
				report.error(type + ": " + message(e));
			}
		});
		report.add(attributes.stream().map(Attribute::result).toList());
		return report;
	}

	private void backfillType(String type, List<Attribute> typeAttributes, boolean dryRun,
			ReferenceBackfillReport report) {
		Class<?> domainClass = registry.domainClass(type)
				.orElseThrow(() -> new IllegalStateException("No domain class maps " + type + "."));
		Set<URI> seen = new HashSet<>();
		int offset = 0;
		long brokerCount;
		do {
			EntitySource.Page page = source.page(type, offset, pageSize);
			brokerCount = page.totalCount();
			List<Owner> owners = new ArrayList<>();
			for (EntityVO entity : page.entities()) {
				if (seen.add(entity.getId())) {
					read(entity, domainClass, report).ifPresent(owners::add);
				}
			}
			backfillPage(owners, typeAttributes, domainClass, dryRun, report);
			if (page.entities().isEmpty()) {
				break;
			}
			offset += page.entities().size();
		} while (offset < brokerCount);
		if (seen.size() < brokerCount) {
			report.error(String.format("%s: read %d distinct ids, but the broker counts %d; re-run to cover the rest.",
					type, seen.size(), brokerCount));
		}
	}

	private Optional<Owner> read(EntityVO entity, Class<?> domainClass, ReferenceBackfillReport report) {
		try {
			return Optional.of(new Owner(entity, document(source.toDomain(entity, domainClass)),
					new LinkedHashMap<>()));
		} catch (RuntimeException e) {
			log.warn("Was not able to read {}.", entity.getId(), e);
			report.error(entity.getId() + ": " + message(e));
			return Optional.empty();
		}
	}

	private void backfillPage(List<Owner> owners, List<Attribute> typeAttributes, Class<?> domainClass,
			boolean dryRun, ReferenceBackfillReport report) {
		Map<String, JsonNode> targets = targets(owners, typeAttributes, report);
		for (Owner owner : owners) {
			for (Attribute attribute : typeAttributes) {
				JsonNode value = owner.document().get(attribute.name);
				if (value == null) {
					continue;
				}
				if (attribute.rewrite) {
					owner.changes().put(attribute, value);
					continue;
				}
				JsonNode filled = fill(value, attribute, targets);
				if (!filled.equals(value)) {
					owner.changes().put(attribute, filled);
				}
			}
			try {
				write(owner, domainClass, dryRun);
			} catch (RuntimeException e) {
				log.warn("Was not able to backfill {}.", owner.entity().getId(), e);
				report.error(owner.entity().getId() + ": " + message(e));
			}
		}
	}

	// one id query per batch of the page's referenced ids, not one per reference
	private Map<String, JsonNode> targets(List<Owner> owners, List<Attribute> typeAttributes,
			ReferenceBackfillReport report) {
		Set<URI> ids = new LinkedHashSet<>();
		for (Owner owner : owners) {
			for (Attribute attribute : typeAttributes) {
				if (!attribute.rewrite) {
					references(owner.document().get(attribute.name)).stream()
							.map(ReferenceFiller::referenceId)
							.filter(Objects::nonNull)
							.map(URI::create)
							.forEach(ids::add);
				}
			}
		}
		Map<String, JsonNode> targets = new HashMap<>();
		List<URI> batch = new ArrayList<>();
		for (URI id : ids) {
			batch.add(id);
			if (batch.size() == TARGET_BATCH_SIZE) {
				fetchTargets(batch, targets, report);
				batch.clear();
			}
		}
		fetchTargets(batch, targets, report);
		return targets;
	}

	private void fetchTargets(Collection<URI> ids, Map<String, JsonNode> targets, ReferenceBackfillReport report) {
		for (EntityVO target : source.entities(ids)) {
			// a type no domain class maps is no TMForum entity; the reference stays as it is, like a missing target
			registry.domainClass(target.getType()).ifPresent(targetClass -> {
				try {
					targets.put(target.getId().toString(), document(source.toDomain(target, targetClass)));
				} catch (RuntimeException e) {
					log.warn("Was not able to read the target {}.", target.getId(), e);
					report.error("target " + target.getId() + ": " + message(e));
				}
			});
		}
	}

	private JsonNode fill(JsonNode value, Attribute attribute, Map<String, JsonNode> targets) {
		if (!value.isArray()) {
			return fillReference(value, attribute, targets);
		}
		ArrayNode filled = jsonMapper.createArrayNode();
		value.forEach(reference -> filled.add(fillReference(reference, attribute, targets)));
		return filled;
	}

	private static JsonNode fillReference(JsonNode reference, Attribute attribute, Map<String, JsonNode> targets) {
		String id = ReferenceFiller.referenceId(reference);
		if (id == null) {
			return reference;
		}
		JsonNode target = targets.get(id);
		if (target == null) {
			attribute.missingTargets++;
			return reference;
		}
		JsonNode filled = ReferenceFiller.fill(reference, target, attribute.fields);
		if (filled != reference) {
			attribute.filledReferences++;
		}
		return filled;
	}

	private void write(Owner owner, Class<?> domainClass, boolean dryRun) {
		if (owner.changes().isEmpty()) {
			return;
		}
		Object update = partial(owner, owner.changes(), domainClass);
		EntityVO written = javaObjectMapper.toEntityVO(update, NgsiLdWriteOperation.UPDATE);
		int planned = owner.changes().size();
		Map<Attribute, JsonNode> replaced = new LinkedHashMap<>();
		for (Attribute attribute : List.copyOf(owner.changes().keySet())) {
			boolean sameInstances = datasetIds(owner.entity(), attribute.name).equals(datasetIds(written, attribute.name));
			if (attribute.rewrite && sameInstances) {
				// already in the new format
				owner.changes().remove(attribute);
			} else if (!sameInstances && !replaceOnUpdate) {
				replaced.put(attribute, owner.document().get(attribute.name));
			}
		}
		if (owner.changes().isEmpty()) {
			return;
		}
		if (!dryRun) {
			URI id = owner.entity().getId();
			if (replaceByUpdateAttributes && !replaced.isEmpty()) {
				writeReplacing(id, owner, replaced.keySet(), domainClass);
			} else {
				Object finalUpdate = owner.changes().size() == planned
						? update
						: partial(owner, owner.changes(), domainClass);
				replaced.keySet().forEach(attribute -> source.deleteAttribute(id, attribute.name));
				try {
					repository.updateDomainEntity(id.toString(), finalUpdate).block();
				} catch (RuntimeException e) {
					throw restore(owner, replaced, domainClass, e);
				}
			}
		}
		owner.changes().forEach((attribute, value) -> {
			attribute.changedEntities++;
			if (attribute.rewrite) {
				attribute.filledReferences += references(value).size();
			}
		});
	}

	// Update Attributes replaces every instance of an attribute the entity already has; the others are appended
	private void writeReplacing(URI id, Owner owner, Set<Attribute> replaced, Class<?> domainClass) {
		Map<Attribute, JsonNode> replacing = new LinkedHashMap<>(owner.changes());
		replacing.keySet().retainAll(replaced);
		Map<Attribute, JsonNode> appended = new LinkedHashMap<>(owner.changes());
		appended.keySet().removeAll(replaced);
		EntityVO replacement = javaObjectMapper.toEntityVO(partial(owner, replacing, domainClass),
				NgsiLdWriteOperation.UPDATE);
		HttpResponse<?> response = repository.updateAttributes(id, ngsiMapper.map(replacement)).block();
		if (response != null && response.getStatus() == HttpStatus.MULTI_STATUS) {
			throw new IllegalStateException(String.format("The broker did not replace %s: %s",
					names(replacing), response.getBody(String.class).orElse("no details given")));
		}
		if (!appended.isEmpty()) {
			repository.updateDomainEntity(id.toString(), partial(owner, appended, domainClass)).block();
		}
	}

	// rewrites what the failed write deleted, from the read values, so a failure does not lose data
	private RuntimeException restore(Owner owner, Map<Attribute, JsonNode> deleted, Class<?> domainClass,
			RuntimeException failure) {
		if (deleted.isEmpty()) {
			return failure;
		}
		try {
			repository.updateDomainEntity(owner.entity().getId().toString(), partial(owner, deleted, domainClass))
					.block();
			return new IllegalStateException(String.format("write failed, the deleted attributes %s were restored: %s",
					names(deleted), message(failure)), failure);
		} catch (RuntimeException restoreFailure) {
			failure.addSuppressed(restoreFailure);
			return new IllegalStateException(String.format(
					"write failed and restoring the deleted attributes failed too, restore them by hand from %s: %s",
					deletedValues(deleted), message(failure)), failure);
		}
	}

	// a document holding only id, type and the given attributes maps to an object that writes nothing else
	private Object partial(Owner owner, Map<Attribute, JsonNode> values, Class<?> domainClass) {
		ObjectNode partial = jsonMapper.createObjectNode();
		partial.set(ReferenceFiller.ID_KEY, owner.document().get(ReferenceFiller.ID_KEY));
		if (owner.document().has(TYPE_KEY)) {
			partial.set(TYPE_KEY, owner.document().get(TYPE_KEY));
		}
		values.forEach((attribute, value) -> partial.set(attribute.name, value));
		return documents.fromDocument(jsonMapper.writeValueAsString(partial), domainClass, StorageOperation.UPDATE);
	}

	private ObjectNode document(Object domainEntity) {
		return (ObjectNode) jsonMapper.readTree(documents.toDocument(domainEntity, DocumentMode.FULL,
				StorageOperation.READ));
	}

	private static List<JsonNode> references(JsonNode value) {
		if (value == null) {
			return List.of();
		}
		if (!value.isArray()) {
			return List.of(value);
		}
		List<JsonNode> references = new ArrayList<>();
		value.forEach(references::add);
		return references;
	}

	/**
	 * The datasetIds of the attribute's instances, sorted; {@value #DEFAULT_INSTANCE} stands for the default instance.
	 */
	static List<String> datasetIds(EntityVO entity, String attribute) {
		AdditionalPropertyVO value = entity.getAdditionalProperties() == null
				? null
				: entity.getAdditionalProperties().get(attribute);
		List<?> instances = value instanceof List<?> list ? list : value == null ? List.of() : List.of(value);
		return instances.stream()
				.map(BrokerReferenceBackfill::datasetId)
				.sorted()
				.toList();
	}

	private static String datasetId(Object instance) {
		URI datasetId = switch (instance) {
			case RelationshipVO relationship -> relationship.getDatasetId();
			case PropertyVO property -> property.getDatasetId();
			case GeoPropertyVO geoProperty -> geoProperty.getDatasetId();
			default -> null;
		};
		return datasetId == null ? DEFAULT_INSTANCE : datasetId.toString();
	}

	private static List<String> names(Map<Attribute, JsonNode> attributes) {
		return attributes.keySet().stream().map(attribute -> attribute.name).toList();
	}

	private static String deletedValues(Map<Attribute, JsonNode> deleted) {
		Map<String, JsonNode> values = new LinkedHashMap<>();
		deleted.forEach((attribute, value) -> values.put(attribute.name, value));
		return values.toString();
	}

	private static String message(Exception e) {
		return Objects.requireNonNullElse(e.getMessage(), e.getClass().getName());
	}
}
