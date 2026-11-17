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
     * list. Observed on Orion-LD 1.9.0 in append mode. Scorpio 5.0.3 reads back {@code []} (its contract IT does
     * not record this); replace mode is not run against a broker.
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
     * An equality filter whose string literal holds {@code &} or {@code ;} matches nothing, even the entity storing
     * exactly that literal, and one holding {@code ,} without those fails the list (the broker answers 500).
     * Observed on Scorpio 5.0.3 for {@code Catalog.name}, Orion-LD 1.9.0 matches; pending acceptance (ticket 0).
     * Derived, not observed (the hub API ITs do not run on Scorpio): the hub duplicate check puts the unescaped
     * callback into the same kind of filter, so a callback holding {@code ,} is expected to fail the registration
     * and one holding {@code &} or {@code ;} to register twice without a 409.
     */
    SEPARATOR_IN_LITERAL_NEVER_MATCHES,

    /**
     * An equality filter on a list, of plain values or of references by their id, only matches a list of one
     * element: the mapping stores a longer list as one attribute instance per element (datasetIds), which the
     * broker's {@code q} does not reach. A hub taking several entity types or event types is thus never found by the
     * hub lookup. Observed on Orion-LD 1.9.0 by {@code HubLookupIT} (plain values) and by the contract's
     * reference-id and string-list filter cases; Scorpio 5.0.3 and PostgreSQL 18 match any element. Pending
     * acceptance (ticket 0).
     */
    MULTI_ELEMENT_LIST_NEVER_MATCHES,

    /**
     * A regex anchored with {@code ^} or {@code $} matches nothing, while an unanchored one matches (the contract's
     * {@code REGEX_UNANCHORED} case). Observed on
     * Scorpio 5.0.3, quoted and unquoted. The not-equals idiom still works where {@code optimizeRegexNotEquals}
     * rewrites it to {@code !=}. Pending acceptance (ticket 0).
     */
    ANCHORED_REGEX_NEVER_MATCHES,

    /**
     * A regex holding {@code |}, {@code ,}, {@code ;}, {@code (} or {@code )} (an alternation or a group) fails the
     * list instead of matching: the NGSI-LD renderer rejects it with a 400, because Orion-LD 1.9.0 and Scorpio 5.0.3
     * read these characters as {@code q} syntax, quoted or not ({@code a|b} becomes "matches a, or attribute b
     * exists"; a group answers 400 on Orion-LD and matches nothing on Scorpio). Pending acceptance (ticket 0).
     */
    REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED,

    /**
     * A string comparison ({@code gt}, {@code lt}, ...) follows a linguistic collation instead of byte order, so
     * {@code "Beta"} is greater than {@code "alpha"}. Observed on Scorpio 5.0.3; Orion-LD 1.9.0 and PostgreSQL 18
     * compare bytes. Pending acceptance (ticket 0).
     */
    STRING_COMPARISON_IS_LINGUISTIC,

    /**
     * A list with a sort comes back in the order it would have without one, whatever the keys and directions.
     * Observed on Scorpio 5.0.3, which has no {@code orderBy} parameter and always orders by creation, and on
     * Orion-LD 1.9.0, which takes {@code orderBy} only with {@code local=true} and then ignores it unless it runs
     * with {@code ORIONLD_EXPERIMENTAL} (one key, descending only via {@code reverse=true}). Pending acceptance
     * (ticket 0).
     */
    SORT_IS_IGNORED,

    /**
     * A filter holding a number and a boolean comparison, AND-ed or OR-ed, fails the list: the broker answers 500
     * ("ARRAY types boolean and jsonb cannot be matched") whatever the order, parentheses or quoting. Observed on
     * Scorpio 5.0.3 by {@code ListQueryIT}; Orion-LD 1.9.0 and PostgreSQL 18 answer it. Pending acceptance (ticket 0).
     */
    NUMBER_AND_BOOLEAN_FILTER_FAILS,

    /**
     * {@code findEntitiesPolymorphic} with several ids and several types fails with (LIST, UNKNOWN).
     */
    SEVERAL_IDS_WITH_SEVERAL_TYPES_FAIL
}
