package org.fiware.tmforum.storage.contract;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * An {@link EntityFixture} whose entities also serve the update cases: partial entities that carry only one of a
 * mutable attribute, an untouched attribute and a list.
 */
public interface UpdateFixture<T> extends EntityFixture<T> {

    /**
     * Carries only the mutable attribute, with a value that differs from the one of {@link #newEntity}.
     */
    T mutate(URI id);

    /**
     * Carries only the list, emptied.
     */
    T emptiedList(URI id);

    Object mutableValue(T entity);

    /**
     * Never carried by an update, so it must keep the value of {@link #newEntity}.
     */
    Object untouchedValue(T entity);

    /**
     * Null when the list reads back absent.
     */
    List<?> listValue(T entity);

    /**
     * Adds to the entity an extension (unknown) property named like the mutable attribute, with a value that
     * differs from the ones of {@link #newEntity} and {@link #mutate}, and returns it.
     */
    T withExtensionNamedLikeTheMutableAttribute(T entity);

    /**
     * The value {@link #withExtensionNamedLikeTheMutableAttribute} gives its extension.
     */
    Object extensionValue();

    /**
     * Names of the entity's extension properties, empty when it has none.
     */
    Set<String> extensionNames(T entity);

    /**
     * Adds an extension (unknown) property to the entity and returns it.
     */
    T withExtension(T entity, String name, Object value);

    /**
     * Null when the entity has no extension of that name.
     */
    Object extensionValue(T entity, String name);

    /**
     * An update carrying only the object attribute, with no field set, so it serializes as an empty object.
     */
    T emptiedObject(URI id);

    /**
     * The object attribute; {@link #newEntity} sets it.
     */
    Object objectValue(T entity);
}
