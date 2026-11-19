package org.fiware.tmforum.storage.contract;

import java.net.URI;

/**
 * Entities of a class that is not versionable, so a {@code version} can only be an extension (unknown) property.
 * Its class may differ from the contract's main class.
 */
public interface UnversionedFixture<U> extends EntityFixture<U> {

    /**
     * A complete entity carrying {@code version} as an extension property.
     */
    U withExtensionVersion(URI id, String version);
}
