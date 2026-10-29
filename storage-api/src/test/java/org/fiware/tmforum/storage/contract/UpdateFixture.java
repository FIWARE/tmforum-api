package org.fiware.tmforum.storage.contract;

import java.net.URI;
import java.util.List;

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
}
