package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class to be used for all generated VOs. Will preserve all properties that are not explicitly mapped.
 */
public class UnknownPreservingBase {

	private Map<String, Object> unknownProperties = new HashMap<>();

	@JsonAnyGetter
	public Map<String, Object> getUnknownProperties() {
		return this.unknownProperties;
	}

	@JsonAnySetter
	public void setUnknownProperties(String propertyKey, Object value) {
		if (this.unknownProperties == null) {
			this.unknownProperties = new HashMap();
		}

		this.unknownProperties.put(propertyKey, value);
	}

	@JsonIgnore
	public UnknownPreservingBase unknownProperties(Map<String, Object> unknownProperties) {
		this.unknownProperties = unknownProperties;
		return this;
	}

	@JsonIgnore
	public <C> C getAtSchemaLocation() {
		return null;
	}
}
