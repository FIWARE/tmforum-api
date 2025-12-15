package org.fiware.tmforum.product;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.service.CharacteristicValueSpecification;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ProductSpecificationCharacteristic {

	private String tmfId;
	private Boolean configurable;
	private String description;
	private Boolean extensible;
	private Boolean isUnique;
	private Integer maxCardinality;
	private Integer minCardinality;
	private String name;
	private String regex;
	private String valueType;
	private List<ProductSpecificationCharacteristicRelationship> productSpecCharRelationship;
	private List<CharacteristicValueSpecification> productSpecCharacteristicValue;
	private TimePeriod validFor;
	private String atValueSchemaLocation;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;

	@JsonIgnore
	private Map<String, Object> additionalProperties = new HashMap<>();

	@JsonAnyGetter
	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	@JsonAnySetter
	public void setAdditionalProperty(String propertyKey, Object value) {
		if (this.additionalProperties == null) {
			this.additionalProperties = new HashMap<>();
		}
		this.additionalProperties.put(propertyKey, value);
	}

	@JsonIgnore
	public void addAdditionalProperty(String propertyKey, Object value) {
		if (this.additionalProperties == null) {
			this.additionalProperties = new HashMap<>();
		}
		this.additionalProperties.put(propertyKey, value);
	}
}
