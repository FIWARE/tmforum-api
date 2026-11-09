package org.fiware.tmforum.storage.contract;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Entities of the contract's main class whose references point at ids the backend does not hold, every payload
 * field set, the extra fields of reference subclasses included. Drives the inline-reference cases.
 */
public interface ReferenceFixture<T> {

    /**
     * Carries at least two reference attributes.
     */
    T withReferences(URI id);

    /**
     * Carries only one of the reference attributes of {@link #withReferences}, with references to other ids.
     */
    T withOneReferenceReplaced(URI id);

    /**
     * Reference payloads keyed by attribute, comparable with {@code equals}; an attribute the entity does not carry
     * is left out.
     */
    Map<String, List<?>> references(T entity);

    /**
     * Id of a reference of {@code entity} whose target type the backend stores, so a same-id {@link #target} can
     * be created.
     */
    URI targetId(T entity);

    /**
     * A target entity carrying only {@code name}, so it serves both its creation and its rename.
     */
    Object target(URI id, String name);
}
