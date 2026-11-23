package org.fiware.tmforum.migration.postgresql;

import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.migration.postgresql.VerifyReport.TypeReport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Checks a finished import type by type: counts, every entity's top-level attributes (raw check), a field-level
 * diff of a seeded sample (or of every entity of a small type), and the escape check (no
 * {@value RawAttributeCheck#ESCAPE_PREFIX} key anywhere, hub queries stored decoded). Reads only, on both sides.
 */
@Slf4j
class VerifyCommand {

	static final List<String> HUB_QUERY_ATTRIBUTES = List.of("query", "rawQuery");
	private static final String VALUE_KEY = "value";

	private final EntitySource source;
	private final VerifyTarget target;
	private final EntityTypeRegistry registry;
	private final ReferenceBackfillCatalog catalog;
	private final VerifyOptions options;
	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	VerifyCommand(EntitySource source, VerifyTarget target, EntityTypeRegistry registry,
			ReferenceBackfillCatalog catalog, VerifyOptions options) {
		this.source = source;
		this.target = target;
		this.registry = registry;
		this.catalog = catalog;
		this.options = options;
	}

	VerifyReport run(Collection<String> types) {
		VerifyReport report = new VerifyReport(options);
		Map<String, Long> targetCounts;
		try {
			targetCounts = target.currentCounts();
		} catch (RuntimeException e) {
			log.warn("Was not able to count the target's rows.", e);
			report.error("Was not able to count the target's rows: " + e.getMessage());
			return report;
		}
		for (String type : types) {
			TypeReport typeReport = report.type(type);
			typeReport.targetCount = targetCounts.getOrDefault(type, 0L);
			try {
				verifyType(type, typeReport);
			} catch (RuntimeException e) {
				log.warn("Was not able to verify {}.", type, e);
				typeReport.error = Objects.requireNonNullElse(e.getMessage(), e.getClass().getName());
			}
		}
		try {
			report.escapedKeyRows(target.rowsWithEscapedKeys(VerifyReport.MAX_EXAMPLES));
		} catch (RuntimeException e) {
			log.warn("Was not able to run the escape check.", e);
			report.error("Was not able to run the escape check: " + e.getMessage());
		}
		return report;
	}

	private void verifyType(String type, TypeReport report) {
		Class<?> domainClass = registry.domainClass(type)
				.orElseThrow(() -> new IllegalArgumentException("No domain class maps " + type + "."));
		RawAttributeCheck rawCheck = new RawAttributeCheck(type, domainClass, options.direction());
		DocumentDiff.Rules rules = DocumentDiff.Rules.of(type, domainClass, catalog, options.direction());
		boolean hub = TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION.equals(type);
		EntitySample sample = new EntitySample(options.sampleSize(), options.seed());
		Set<URI> seen = new HashSet<>();
		int offset = 0;
		do {
			EntitySource.Page page = source.page(type, offset, options.pageSize());
			if (offset == 0) {
				report.brokerCount = page.totalCount();
				report.fullCheck = page.totalCount() <= options.fullCheckThreshold();
			}
			List<EntityVO> entities = page.entities().stream().filter(entity -> seen.add(entity.getId())).toList();
			List<EntityVO> stored = checkAttributes(entities, rawCheck, report);
			if (hub) {
				checkHubQueries(stored, report);
			}
			if (report.fullCheck) {
				diff(stored, domainClass, rules, report);
			} else {
				stored.forEach(sample::offer);
			}
			if (page.listed() == 0) {
				break;
			}
			offset += page.listed();
		} while (offset < report.brokerCount);
		if (!report.fullCheck) {
			diff(sample.entities(), domainClass, rules, report);
		}
	}

	// returns the entities that are stored, the others are reported as missing rows
	private List<EntityVO> checkAttributes(List<EntityVO> entities, RawAttributeCheck rawCheck, TypeReport report) {
		Map<URI, VerifyTarget.StoredKeys> keys = target.topLevelKeys(entities.stream().map(EntityVO::getId).toList());
		return entities.stream().filter(entity -> {
			VerifyTarget.StoredKeys targetKeys = keys.get(entity.getId());
			if (targetKeys == null) {
				report.missingRows.add(entity.getId().toString());
				return false;
			}
			report.rawCheck(entity.getId(), rawCheck.check(RawAttributeCheck.attributeNames(entity), targetKeys));
			return true;
		}).toList();
	}

	private void checkHubQueries(List<EntityVO> hubs, TypeReport report) {
		Map<URI, String> documents = target.storedDocuments(hubs.stream().map(EntityVO::getId).toList());
		for (EntityVO hub : hubs) {
			String document = documents.get(hub.getId());
			if (document == null) {
				// deleted since the raw check, the counts report it
				continue;
			}
			JsonNode stored = jsonMapper.readTree(document);
			for (String attribute : HUB_QUERY_ATTRIBUTES) {
				String expected = expectedStoredQuery(rawValue(hub, attribute), options.sourceEscaped());
				JsonNode storedValue = stored.get(attribute);
				String actual = storedValue == null || storedValue.isNull() ? null : storedValue.asString();
				if (!Objects.equals(expected, actual)) {
					report.hubQueryMismatches.add(String.format("%s %s: stored %s, expected %s", hub.getId(),
							attribute, actual, expected));
				}
			}
		}
	}

	/**
	 * The value a hub query must be stored as: {@code ForbiddenCharacterEscaper.unescape} of the broker's value when the
	 * broker data is escaped, the value itself otherwise. Decoding, not a pattern match, so a literal {@code %3D} in the
	 * original filter (stored by the broker as {@code %253D}) is expected back as {@code %3D}.
	 */
	static String expectedStoredQuery(String brokerValue, boolean sourceEscaped) {
		return brokerValue == null || !sourceEscaped ? brokerValue : URLDecoder.decode(brokerValue, StandardCharsets.UTF_8);
	}

	/**
	 * The plain value of a top-level property of a raw broker entity, or null.
	 */
	static String rawValue(EntityVO entity, String attribute) {
		Object property = entity.getAdditionalProperties() == null ? null : entity.getAdditionalProperties().get(attribute);
		Object value = switch (property) {
			case PropertyVO propertyVO -> propertyVO.getValue();
			case Map<?, ?> map -> map.get(VALUE_KEY);
			case null, default -> null;
		};
		return value == null ? null : value.toString();
	}

	private void diff(List<EntityVO> entities, Class<?> domainClass, DocumentDiff.Rules rules, TypeReport report) {
		if (entities.isEmpty()) {
			return;
		}
		Map<URI, String> documents = target.storedDocuments(entities.stream().map(EntityVO::getId).toList());
		for (EntityVO entity : entities) {
			String document = documents.get(entity.getId());
			if (document == null) {
				continue;
			}
			report.diffChecked++;
			try {
				report.differences(entity.getId(), DocumentDiff.diff(
						target.document(source.toDomain(entity, domainClass)),
						target.document(target.domainEntity(document, domainClass)), rules));
			} catch (RuntimeException e) {
				log.warn("Was not able to diff {}.", entity.getId(), e);
				report.mappingFailures.add(entity.getId() + ": " + e.getMessage());
			}
		}
	}
}
