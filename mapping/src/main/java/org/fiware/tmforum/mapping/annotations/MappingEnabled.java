package org.fiware.tmforum.mapping.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the given object can be mapped to an NGSI-LD Entity
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MappingEnabled {

	/**
	 * Supported entity types for the mapping. E.g. which ngsi-ld entity types can be mapped to the object.
	 */
	String[] entityType() default {""};
}
