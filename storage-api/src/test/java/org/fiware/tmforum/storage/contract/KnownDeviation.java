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
     * An equality filter on a list of plain values only matches a list of one element: the mapping stores a longer
     * list as one attribute instance per element (datasetIds), which the broker's {@code q} does not reach. A hub
     * taking several entity types or event types is thus never found by the hub lookup. Observed by {@code HubLookupIT}
     * on Orion-LD 1.9.0; Scorpio 5.0.3 and PostgreSQL 18 match any element there. Pending acceptance (ticket 0).
     */
    MULTI_ELEMENT_LIST_NEVER_MATCHES,

    /**
     * An equality filter with several values fails with (LIST, UNKNOWN). Orion-LD 1.9.0 rejects (400) the orion-ld
     * profile's {@code attr==("a";"b")} ({@code ngsildOrQueryValue: ";"}, {@code encloseQuery: true}) but accepts
     * {@code attr=="a","b"}; Scorpio 5.0.3 fails (500) on the scorpio profile's {@code attr=="a","b"}. Pending
     * acceptance (ticket 0).
     */
    VALUE_LIST_FAILS,

    /**
     * {@code AnyOf} matches like {@code AllOf}: the orion-ld profile ({@code ngsildOrQueryKey: ";"}) joins its
     * filters with {@code ;}, NGSI-LD's AND. Observed on Orion-LD 1.9.0, which ORs them with {@code |}. Pending
     * acceptance (ticket 0).
     */
    ANY_OF_MATCHES_AS_ALL_OF,

    /**
     * {@code AnyOf} breaks: the scorpio profile ({@code ngsildOrQueryKey: ","}) joins its filters with {@code ,},
     * which Scorpio 5.0.3 does not read as OR. Over comparisons the list fails with (LIST, UNKNOWN) (500), over
     * {@code AllOf} groups it matches nothing. Scorpio ORs with {@code |}. Pending acceptance (ticket 0).
     */
    ANY_OF_FAILS_OR_MATCHES_NOTHING,

    /**
     * A regex filter fails with (LIST, UNKNOWN): the renderer quotes the pattern, which Orion-LD 1.9.0
     * rejects (400), while it accepts it unquoted. The not-equals idiom still works where
     * {@code optimizeRegexNotEquals} rewrites it to {@code !=}. Pending acceptance (ticket 0).
     */
    REGEX_FAILS,

    /**
     * A regex anchored with {@code ^} or {@code $} matches nothing, while an unanchored one matches. Observed on
     * Scorpio 5.0.3, quoted and unquoted. The not-equals idiom still works where {@code optimizeRegexNotEquals}
     * rewrites it to {@code !=}. Pending acceptance (ticket 0).
     */
    ANCHORED_REGEX_NEVER_MATCHES,

    /**
     * A list with a sort fails with (LIST, UNKNOWN): Orion-LD 1.9.0 rejects {@code orderBy} without
     * {@code local=true} (501). Pending acceptance (ticket 0).
     */
    SORT_FAILS,

    /**
     * A list with a sort comes back in the order it would have without one, whatever the keys and directions.
     * Observed on Scorpio 5.0.3. Pending acceptance (ticket 0).
     */
    SORT_IS_IGNORED,

    /**
     * {@code findEntitiesPolymorphic} with several ids and several types fails with (LIST, UNKNOWN).
     */
    SEVERAL_IDS_WITH_SEVERAL_TYPES_FAIL
}
