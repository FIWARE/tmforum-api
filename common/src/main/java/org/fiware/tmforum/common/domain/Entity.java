package org.fiware.tmforum.common.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;

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
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "atSchemaLocation", fromProperties = true) }))
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
