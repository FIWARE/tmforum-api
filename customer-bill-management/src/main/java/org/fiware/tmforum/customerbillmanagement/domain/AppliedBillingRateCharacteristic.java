package org.fiware.tmforum.customerbillmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = "applied-billing-rate-characteristic")
public class AppliedBillingRateCharacteristic extends Entity {

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
	private String name;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "valueType")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "valueType")}))
	private String valueType;

	// "tmfValue" avoids clash with the NGSI-LD "value" keyword.
	// getTmfValue()/setTmfValue() are used by MapStruct (@Mapping(source="tmfValue")).
	// getValue()/setValue() bridge the query path "value" → NGSI-LD "tmfValue" via @AttributeGetter,
	// and are hidden from Jackson with @JsonIgnore to avoid a duplicate "value" key.
	private Object tmfValue;

	@JsonIgnore
	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "tmfValue")
	public Object getValue() {
		return tmfValue;
	}

	@JsonIgnore
	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "tmfValue", targetClass = Object.class)
	public void setValue(Object value) {
		this.tmfValue = value;
	}
}
