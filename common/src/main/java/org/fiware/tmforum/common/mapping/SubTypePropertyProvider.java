package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Provider interface for sub-type property discovery. Implementations register
 * known sub-type VO classes so that the {@link ValidatingDeserializer} can distinguish
 * known sub-type properties from truly unknown extension properties.
 *
 * <p>When an incoming payload uses {@code @type} to indicate a sub-type (e.g. "LogicalResource"),
 * the sub-type-specific fields end up as unknown properties on the parent VO. Without a provider,
 * the deserializer would reject those fields unless {@code @schemaLocation} is supplied.
 * A registered provider tells the deserializer which property names are legitimate sub-type fields,
 * allowing them through without requiring an explicit JSON Schema.</p>
 */
public interface SubTypePropertyProvider {

	/**
	 * Return the set of known JSON property names for the given TMForum {@code @type} value,
	 * or {@link Optional#empty()} if the type is not recognized by this provider.
	 *
	 * @param atType the TMForum {@code @type} value (e.g. "LogicalResource", "InstalledSoftware")
	 * @return the known property names, or empty if the type is not recognized
	 */
	Optional<Set<String>> getKnownProperties(String atType);

	/**
	 * Resolve all JSON property names declared on the given VO class and its superclasses,
	 * up to (but excluding) {@link UnknownPreservingBase}. Uses the {@link JsonProperty}
	 * annotation value as the canonical property name.
	 *
	 * @param voClass the generated VO class to introspect
	 * @return the set of JSON property names
	 */
	static Set<String> resolveJsonProperties(Class<?> voClass) {
		Set<String> names = new HashSet<>();
		Class<?> current = voClass;
		while (current != null && current != UnknownPreservingBase.class && current != Object.class) {
			for (Field field : current.getDeclaredFields()) {
				JsonProperty annotation = field.getAnnotation(JsonProperty.class);
				if (annotation != null && !annotation.value().isEmpty()) {
					names.add(annotation.value());
				}
			}
			current = current.getSuperclass();
		}
		return Set.copyOf(names);
	}
}
