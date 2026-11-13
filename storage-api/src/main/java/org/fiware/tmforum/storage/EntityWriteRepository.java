package org.fiware.tmforum.storage;

import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Write side of the entity port. Each method completes empty on success and fails with a
 * {@link StorageException} carrying its own {@link StorageOperation}.
 */
public interface EntityWriteRepository {

    /**
     * Fails with {@link StorageErrorReason#CONFLICT} when the id exists and {@link StorageErrorReason#INVALID_DATA}
     * when the backend rejects the content. An extension (unknown) property named like a declared domain property
     * (domain names, e.g. {@code atType}) is dropped, the declared value is stored. An extension with a null value
     * is not written.
     * <p>
     * Known NGSI-LD adapter defect, not a contract rule (ticket 0): the mapping library writes the extension
     * property over the declared one, so the declared value is lost.
     * <p>
     * Open differences, not contract rules and not pinned by a contract case (ticket 0): the PostgreSQL adapter also
     * drops an extension named like an ignored domain accessor (e.g. {@code value}, {@code entityState}), which the
     * NGSI-LD adapter presumably keeps; the NGSI-LD mapping library writes a null extension as an empty object
     * ({@code {}}). Both are read from the code, not checked against a broker.
     */
    <T> Mono<Void> createDomainEntity(T domainEntity);

    /**
     * Writes only the attributes the entity carries, never a whole-document overwrite: null fields stay
     * untouched, an emptied list is cleared and reads back absent or empty, so callers must accept both. A carried
     * list replaces the stored one. A carried empty object leaves the stored one untouched; an emptied extension
     * list reads back empty (Orion-LD append-mode defect, ticket 0: it reads back as a map from the expanded name
     * to {@code []}).
     * Fails with {@link StorageErrorReason#NOT_FOUND} for an absent id and never creates or completes silently
     * (update-else-create callers rely on it), and with {@link StorageErrorReason#PARTIAL_UPDATE} when the
     * backend wrote only some attributes. Failures carry UPDATE, except the recorded NGSI-LD replace-mode
     * pre-read (see {@link StorageOperation}).
     * <p>
     * Known NGSI-LD adapter defect, not a contract rule (ticket 0): on Orion-LD in append mode (no
     * {@code replaceOnUpdate}) an emptied list whose instances carry a datasetId is not cleared; the broker keeps
     * them and adds an empty instance, so later reads fail. For the same reason any carried list whose instances
     * carry a datasetId is merged into the stored one by datasetId instead of replacing it: a stored instance whose
     * datasetId the update does not carry survives next to the carried ones. For a reference list the datasetId is
     * the reference id, except where a subclass extends it ({@code RelatedParty} {@code <id>:<role>},
     * {@code ProductSpecificationRelationship} {@code <id>:<relationshipType>}, {@code RelatedProductOrderItemRef}
     * {@code <productOrderId>:<orderItemId>}). For a property list it is the position ({@code list-item:n}), so
     * shortening the list presumably keeps the stored tail; that is inferred, not checked against a broker. Only the
     * reference-list case is pinned by a contract case.
     * <p>
     * The extension rules of {@link #createDomainEntity} apply, and so do its NGSI-LD defect and open differences:
     * a null extension leaves the stored one untouched, where the NGSI-LD adapter presumably overwrites it with
     * {@code {}}. No adapter can clear a single extension.
     * <p>
     * Known defect of every adapter, not a contract rule (ticket 0): a primitive field cannot be absent, so an
     * update always carries it and writes its default (e.g. {@code Agreement.documentNumber} reset to 0 by a PATCH
     * that does not carry it). The fix is a boxed type in the domain class.
     */
    <T> Mono<Void> updateDomainEntity(String id, T domainEntity);

    /**
     * Fails with {@link StorageErrorReason#NOT_FOUND} when the id is absent.
     */
    Mono<Void> deleteDomainEntity(URI id);
}
