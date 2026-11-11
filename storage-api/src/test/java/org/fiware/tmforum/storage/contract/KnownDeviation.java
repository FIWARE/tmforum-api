package org.fiware.tmforum.storage.contract;

/**
 * A recorded adapter defect (ticket 0) for which the contract cases assert today's behaviour instead of the
 * contract, so a fix in the backend or adapter shows up as a failing case rather than a silent skip.
 */
public enum KnownDeviation {

    /**
     * An update that empties a list leaves the entity unreadable: the following {@code get} fails.
     */
    EMPTIED_LIST_BREAKS_READS,

    /**
     * An update that empties an extension list makes the extension read back as a map from the expanded attribute
     * name (e.g. {@code https://uri=etsi=org/ngsi-ld/default-context/<name>}) to an empty list instead of an empty
     * list. Observed on Orion-LD 1.9.0 in append mode; Scorpio not checked.
     */
    EMPTIED_EXTENSION_LIST_READS_BACK_EXPANDED,

    /**
     * {@code findEntities} completes empty instead of emitting a page when the page holds no entity, also when
     * the offset is past the last match.
     */
    EMPTY_PAGE_COMPLETES_EMPTY,

    /**
     * An extension (unknown) property named like a declared property overwrites the declared value, on create
     * and on update, and no extension of that name reads back. Observed on Orion-LD 1.9.0 and Scorpio 5.0.3: the
     * mapping library merges the extension into the {@code EntityVO} before the request reaches the broker.
     */
    EXTENSION_OVERRIDES_DECLARED_PROPERTY,

    /**
     * An update carrying a reference list keeps the stored references whose datasetIds it does not carry, next to
     * the carried ones, instead of replacing the list. The merge key is the datasetId, which is the reference id
     * unless the ref class extends it (e.g. {@code RelatedParty} adds the role).
     */
    REFERENCE_LIST_UPDATE_MERGES,

    /**
     * {@code findEntitiesPolymorphic} with several ids and several types fails with (LIST, UNKNOWN).
     */
    SEVERAL_IDS_WITH_SEVERAL_TYPES_FAIL
}
