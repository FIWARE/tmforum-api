package org.fiware.tmforum.storage.contract;

import java.net.URI;
import java.util.UUID;

/**
 * Builds entities of one persisted class for the port contract tests.
 */
public interface EntityFixture<T> {

    Class<T> entityClass();

    /**
     * Type the backend stores the class as, which is also the type segment of its ids.
     */
    String storedType();

    /**
     * A complete entity; for an {@link UpdateFixture} it carries all three of its attributes, the list non-empty.
     */
    T newEntity(URI id);

    URI id(T entity);

    /**
     * A fresh id in the {@code urn:ngsi-ld:<type>:<uuid>} form the port's {@code get} expects.
     */
    default URI newId() {
        return URI.create(String.format("urn:ngsi-ld:%s:%s", storedType(), UUID.randomUUID()));
    }
}
