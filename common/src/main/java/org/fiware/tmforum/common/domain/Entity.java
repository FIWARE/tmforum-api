package org.fiware.tmforum.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.wistefan.mapping.UnmappedProperty;
import io.github.wistefan.mapping.annotations.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract super class for all entities to be created
 */
@EqualsAndHashCode
@RequiredArgsConstructor
public abstract class Entity {

	@JsonIgnore
	public static final String ADDITIONAL_PROPERTIES_KEY = "additionalProperties";

	/**
	 * When sub-classing, this defines the super-class
	 */
	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "atBaseType", embedProperty = true)}))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "atBaseType", fromProperties = true)}))
	String atBaseType;

	/**
	 * A URI to a JSON-Schema file that defines additional attributes and relationships
	 */
	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "atSchemaLocation", embedProperty = true)}))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "atSchemaLocation", fromProperties = true, targetClass = URI.class)}))
	URI atSchemaLocation;

	/**
	 * When sub-classing, this defines the sub-class entity name
	 */
	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "atType", embedProperty = true)}))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "atType", fromProperties = true)}))
	String atType;

	@JsonIgnore
	public String getEntityState() {
		return "default";
	}

	@Setter(onMethod = @__({@UnmappedPropertiesSetter}))
	@Getter(onMethod = @__({@UnmappedPropertiesGetter}))
	private List<UnmappedProperty> additionalProperties;

	public void addAdditionalProperties(String propertyKey, Object value) {
		UnmappedProperty ap = new UnmappedProperty(propertyKey, value);
		if (this.additionalProperties == null) {
			this.additionalProperties = new ArrayList<>();
		}
		this.additionalProperties.add(ap);
	}

}
