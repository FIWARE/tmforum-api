package org.fiware.tmforum.common.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.net.URI;

/**
 * Abstract super class for all entities to be created
 */
@EqualsAndHashCode
@RequiredArgsConstructor
public abstract class Entity {

	/**
	 * When sub-classing, this defines the super-class
	 */
	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "atBaseType", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "atBaseType", fromProperties = true) }))
	String atBaseType;

	/**
	 * A URI to a JSON-Schema file that defines additional attributes and relationships
	 */
	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "atSchemaLocation", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "atSchemaLocation", fromProperties = true, targetClass = URI.class) }))
	URI atSchemaLocation;

	/**
	 * When sub-classing, this defines the sub-class entity name
	 */
	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "atType", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "atType", fromProperties = true) }))
	String atType;

}
