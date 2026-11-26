package org.fiware.tmforum.storage.contract;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Entities whose references point at ids the backend does not hold, with the extra fields of reference subclasses.
 * Drives the inline-reference cases. Its class may differ from the contract's main class.
 */
public interface ReferenceFixture<R> extends EntityFixture<R> {

    /**
     * Carries at least two reference attributes. The reference at {@link #targetId} omits {@code name}, all others
     * set every payload field.
     */
    @Override
    R newEntity(URI id);

    /**
     * Carries only one of the reference attributes of {@link #newEntity}, with references to other ids.
     */
    R withOneReferenceReplaced(URI id);

    /**
     * Reference payloads keyed by attribute, comparable with {@code equals} and including {@code name}; an attribute
     * the entity does not carry is left out.
     */
    Map<String, List<?>> references(R entity);

    /**
     * Id of the reference of {@code entity} that omits {@code name}; the backend stores its target type, so a
     * same-id {@link #target} can be created.
     */
    URI targetId(R entity);

    /**
     * A target entity carrying only {@code name}, so it serves both its creation and its rename.
     */
    Object target(URI id, String name);
}
