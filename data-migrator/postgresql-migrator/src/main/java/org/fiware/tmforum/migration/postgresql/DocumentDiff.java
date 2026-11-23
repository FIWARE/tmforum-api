package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.common.domain.VersionedEntity;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.Entry;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.FillableField;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Compares the source and target documents of one entity, both written by the PostgreSQL document mapper. Only the
 * migration's own changes are allowed: reference fields the backfill filled, the seeded version and the subscription
 * attribute that {@code cleanup-subscriptions} drops; anything else is a mismatch.
 */
final class DocumentDiff {

	static final String VERSION_KEY = "version";
	// longer values are cut in the report, a whole document would drown the cause
	static final int MAX_VALUE_LENGTH = 200;
	private static final String TRUNCATED = "...";
	private static final String ABSENT = "<absent>";

	/**
	 * What a difference is; everything but {@link #MISMATCH} is an intended change of the migration.
	 */
	enum Kind {
		BACKFILLED_REFERENCE_FIELD, SEEDED_VERSION, DROPPED_SUBSCRIPTION, MISMATCH
	}

	/**
	 * One difference at a document path such as {@code relatedParty[1].name}.
	 */
	record Difference(Kind kind, String path, String detail) {

		boolean allowed() {
			return kind != Kind.MISMATCH;
		}
	}

	/**
	 * What may differ for one type. {@code fillableKeys} maps a backfilled attribute to its fillable reference keys.
	 */
	record Rules(boolean versioned, boolean hub, Map<String, Set<String>> fillableKeys) {

		static Rules of(String type, Class<?> domainClass, ReferenceBackfillCatalog catalog) {
			Map<String, Set<String>> fillable = new HashMap<>();
			for (Entry entry : catalog.entries()) {
				if (entry.ownerTypes().contains(type)) {
					fillable.computeIfAbsent(entry.attribute(), attribute -> new TreeSet<>()).addAll(
							entry.fillableFields().stream().map(FillableField::referenceKey).collect(Collectors.toSet()));
				}
			}
			return new Rules(domainClass != null && VersionedEntity.class.isAssignableFrom(domainClass),
					TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION.equals(type), Map.copyOf(fillable));
		}
	}

	private DocumentDiff() {
	}

	/**
	 * Every difference between the documents, allowed or not; empty when they are equal.
	 */
	static List<Difference> diff(ObjectNode source, ObjectNode target, Rules rules) {
		Set<String> keys = new TreeSet<>(source.propertyNames());
		keys.addAll(target.propertyNames());
		List<Difference> differences = new ArrayList<>();
		for (String key : keys) {
			JsonNode sourceValue = source.get(key);
			JsonNode targetValue = target.get(key);
			if (Objects.equals(sourceValue, targetValue)) {
				continue;
			}
			if (VERSION_KEY.equals(key) && rules.versioned() && isBlank(sourceValue)
					&& targetValue != null && ImportCommand.SEED_VERSION.equals(targetValue.asString())) {
				differences.add(new Difference(Kind.SEEDED_VERSION, key, ImportCommand.SEED_VERSION));
			} else if (RawAttributeCheck.SUBSCRIPTION_ATTRIBUTE.equals(key) && rules.hub() && targetValue == null) {
				differences.add(new Difference(Kind.DROPPED_SUBSCRIPTION, key, render(sourceValue)));
			} else if (rules.fillableKeys().containsKey(key)) {
				compareReferences(key, sourceValue, targetValue, rules.fillableKeys().get(key), differences);
			} else {
				differences.add(mismatch(key, sourceValue, targetValue));
			}
		}
		return differences;
	}

	private static void compareReferences(String path, JsonNode source, JsonNode target, Set<String> fillable,
			List<Difference> differences) {
		if (source != null && target != null && source.isArray() && target.isArray()
				&& source.size() == target.size()) {
			for (int i = 0; i < source.size(); i++) {
				compareReference(path + "[" + i + "]", source.get(i), target.get(i), fillable, differences);
			}
			return;
		}
		compareReference(path, source, target, fillable, differences);
	}

	private static void compareReference(String path, JsonNode source, JsonNode target, Set<String> fillable,
			List<Difference> differences) {
		if (Objects.equals(source, target)) {
			return;
		}
		if (source == null || target == null || !source.isObject() || !target.isObject()) {
			differences.add(mismatch(path, source, target));
			return;
		}
		Set<String> keys = new TreeSet<>(source.propertyNames());
		keys.addAll(target.propertyNames());
		for (String key : keys) {
			JsonNode sourceValue = source.get(key);
			JsonNode targetValue = target.get(key);
			if (Objects.equals(sourceValue, targetValue)) {
				continue;
			}
			String keyPath = path + "." + key;
			// the backfill only adds what the stored reference lacks, it never replaces a value
			if (sourceValue == null && fillable.contains(key)) {
				differences.add(new Difference(Kind.BACKFILLED_REFERENCE_FIELD, keyPath, render(targetValue)));
			} else {
				differences.add(mismatch(keyPath, sourceValue, targetValue));
			}
		}
	}

	private static boolean isBlank(JsonNode value) {
		return value == null || value.isNull() || (value.isString() && value.asString().isBlank());
	}

	private static Difference mismatch(String path, JsonNode source, JsonNode target) {
		return new Difference(Kind.MISMATCH, path, "source " + render(source) + ", target " + render(target));
	}

	private static String render(JsonNode value) {
		if (value == null) {
			return ABSENT;
		}
		String text = value.toString();
		return text.length() > MAX_VALUE_LENGTH ? text.substring(0, MAX_VALUE_LENGTH) + TRUNCATED : text;
	}
}
