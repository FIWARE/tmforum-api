package org.fiware.tmforum.common.domain;

import java.util.Set;

/**
 * Marks an entity type whose TMForum {@code version} labels its stored row; every other type stores no version.
 */
public interface VersionedEntity {

    /**
     * The stored entity types of every {@link VersionedEntity} class, subclasses included, for adapters that only see
     * a stored type. Literals, since the classes live in the API modules; a guard test keeps them in sync.
     */
    Set<String> VERSIONED_TYPES = Set.of(
            "agreement",
            "agreementSpecification",
            "api-specification",
            "catalog",
            "category",
            "document-specification",
            "hosting-platform-requirement-specification",
            "logical-resource-specification",
            "physical-resource-specification",
            "product-offering",
            "product-offering-price",
            "product-specification",
            "quote",
            "resource-candidate",
            "resource-catalog",
            "resource-category",
            "resource-specification",
            "service-candidate",
            "service-catalog",
            "service-category",
            "service-specification",
            "software-resource-specification",
            "software-specification",
            "software-support-package-specification",
            "usageSpecification");

    String getVersion();
}
