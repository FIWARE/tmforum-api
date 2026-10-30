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
     * {@code findEntities} completes empty instead of emitting a page when the page holds no entity, also when
     * the offset is past the last match.
     */
    EMPTY_PAGE_COMPLETES_EMPTY,

    /**
     * {@code findEntitiesPolymorphic} with several ids and several types fails with (LIST, UNKNOWN).
     */
    SEVERAL_IDS_WITH_SEVERAL_TYPES_FAIL
}
