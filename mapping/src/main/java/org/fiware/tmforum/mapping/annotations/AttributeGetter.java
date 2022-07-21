package org.fiware.tmforum.mapping.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inidcate that a field should be mapped to an NGIS attribute.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AttributeGetter {

	/**
	 * Type of attribute the field should be mapped to.
	 */
	AttributeType value() default AttributeType.PROPERTY;

	/**
	 * Name of target attribute
	 */
	String targetName();

	/**
	 * In case the entity is used as a relationship, should the field be embedded into
	 * the relationship or is it only relevant for serialization to a full entity?
	 */
	boolean embedProperty() default false;

}


