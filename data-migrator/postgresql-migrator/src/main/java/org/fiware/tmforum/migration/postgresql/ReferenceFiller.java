package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.FillableField;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * The fill-absent rule of the broker backfill, on PostgreSQL-shaped documents; {@code pg_temp.fill_ref} of
 * {@link PostgresReferenceBackfill#CREATE_FILL_REF} is its SQL twin, and both are tested against the same cases.
 */
final class ReferenceFiller {

	static final String ID_KEY = "id";

	private ReferenceFiller() {
	}

	/**
	 * {@code reference} plus every target key it lacks and {@code target} holds non-null; a key present in the
	 * reference, even as JSON null, always wins; an {@code unwrapSingleElement} field takes the element of a one-element
	 * target list and nothing else. Anything but an object reference with an object target comes back as
	 * the same instance, which is never modified.
	 */
	static JsonNode fill(JsonNode reference, JsonNode target, List<FillableField> fields) {
		if (reference == null || !reference.isObject() || target == null || !target.isObject()) {
			return reference;
		}
		ObjectNode filled = null;
		for (FillableField field : fields) {
			JsonNode value = value(target, field);
			if (reference.get(field.referenceKey()) != null || value == null || value.isNull()) {
				continue;
			}
			if (filled == null) {
				filled = ((ObjectNode) reference).deepCopy();
			}
			filled.set(field.referenceKey(), value.deepCopy());
		}
		return filled == null ? reference : filled;
	}

	// a list fills a single-valued reference field only when it has exactly one element
	private static JsonNode value(JsonNode target, FillableField field) {
		JsonNode value = target.get(field.targetKey());
		if (!field.unwrapSingleElement()) {
			return value;
		}
		return value != null && value.isArray() && value.size() == 1 ? value.get(0) : null;
	}

	/**
	 * The target id of an object reference, or null.
	 */
	static String referenceId(JsonNode reference) {
		JsonNode id = reference == null ? null : reference.get(ID_KEY);
		return id != null && id.isString() ? id.asString() : null;
	}
}
