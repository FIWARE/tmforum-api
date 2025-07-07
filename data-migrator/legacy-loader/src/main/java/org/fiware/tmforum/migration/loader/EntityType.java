package org.fiware.tmforum.migration.loader;

/**
 * Holder for the implementing class and the concrete type
 */
public record EntityType(Class entityClass, String entityType) {

	public String toString() {
		return entityClass.toString() + " - " + entityType;
	}
}
