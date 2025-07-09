package org.fiware.tmforum.migration.loader;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import java.io.IOException;
import java.util.Map;

/**
 * In order to not use "reserved" keys from ngsi-ld, nested objects cannot use "id" or "value" as keys. In the domain entities
 * those keys are mapped to "tmfId" or "tmfValue" in the 1.x version. This delegate will translate between those fiels in the old
 * format("id", "value") and the new format("tmfId","tmfValue")
 */
public class FieldRenamingGeneratorDelegate extends JsonGeneratorDelegate {

	public FieldRenamingGeneratorDelegate(JsonGenerator d) {
		super(d);
	}

	@Override
	public void writeFieldName(String name) throws IOException {
		String renamed = rename(name, getOutputContext());
		super.writeFieldName(renamed);
	}

	private String rename(String name, JsonStreamContext context) {
		// since we are working on plain json, we do not see if it's a nested object(that needs to be renamed) or
		// a property(where the fields are allowed).
		// If the context object contains the type "Property", we keep the field names
		if ("id".equals(name) && context.getCurrentValue() instanceof Map valueMap && valueMap.get("type") instanceof String typeString && !typeString.equals("Property")) {
			return "tmfId";
		}

		if ("value".equals(name) && context.getCurrentValue() instanceof Map valueMap && valueMap.get("type") instanceof String typeString && !typeString.equals("Property")) {
			return "tmfValue";
		}
		return name;
	}
}
