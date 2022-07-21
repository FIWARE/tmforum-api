package org.fiware.tmforum.mapping.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a method should be used to set an attribute
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AttributeSetter {

	/**
	 * Type of the attribute.
	 */
	AttributeType value() default AttributeType.PROPERTY;

	/**
	 * Name to be used for an attribute.
	 */
	String targetName();

	/**
	 * Type of the entries to be used when the attribute is a list or a relationship. This is required, since the concrete class is hidden
	 * at runtime due to type-erasure.
	 */
	Class<?> targetClass() default Object.class;

	/**
	 * Should the field be constructed from the properties? Does only apply for AttributeType.RELATIONSHIP or AttributeType.RELATIONSHIP_LIST.
	 * If set to false, the field will be filled from the related entity.
	 */
	boolean fromProperties() default false;

}
