package org.fiware.tmforum.storage.contract;

import java.net.URI;

/**
 * An {@link EntityFixture} of a versionable class; its {@link #newEntity} carries no version.
 */
public interface VersionFixture<T> extends EntityFixture<T> {

    /**
     * A complete entity labelled with {@code version}, also usable as an update.
     */
    T withVersion(URI id, String version);
}
