package org.fiware.tmforum.documentmanagement;

import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service interface for managing document attachments.
 *
 * <p>Implementations may back this with different storage systems (e.g. S3-compatible object
 * storage, local filesystem, a third-party DMS). When no implementation is configured the
 * DocumentManagement API still operates normally, but rejects any attachment that carries inline
 * content — pure URL/href references are always accepted regardless of the backing store.
 */
public interface AttachmentService {

    /**
     * Validates that the attachment's inline content (if any) is acceptable for storage —
     * e.g. correct encoding and within the size limit. Throws a {@code TmForumException} on
     * violation. Attachments that carry only a URL reference are always considered valid.
     *
     * @param attachment the attachment to validate
     */
    void validateAttachmentContent(AttachmentRefOrValue attachment);

    /**
     * Offloads inline base64 content from the given attachments to the backing store. The
     * {@code content} field is cleared and the {@code url} field is set to an implementation-
     * specific internal reference that can later be resolved by {@link #resolveAttachments}.
     * Attachments that already carry a managed URL or that have no content are returned unchanged.
     *
     * @param attachments list of attachments to process
     * @param entityId    the owning entity's ID, used to namespace stored objects
     * @return a {@code Mono} emitting the processed list
     */
    Mono<List<AttachmentRefOrValue>> offloadAttachments(List<AttachmentRefOrValue> attachments, String entityId);

    /**
     * Resolves the internal storage reference in the {@code url} field of each attachment to an
     * accessible URL (e.g. a pre-signed download link) and returns a copy with {@code url} updated
     * accordingly. Attachments whose {@code url} is not managed by this service are returned unchanged.
     *
     * @param attachments list of attachments to resolve
     * @return a {@code Mono} emitting the resolved list
     */
    Mono<List<AttachmentRefOrValue>> resolveAttachments(List<AttachmentRefOrValue> attachments);

    /**
     * Deletes any stored objects that are referenced by the given attachments. Attachments
     * without a retrieval reference are silently ignored.
     *
     * @param attachments list of attachments whose stored content should be removed
     * @return a {@code Mono} that completes when all deletions are done
     */
    Mono<Void> deleteAttachments(List<AttachmentRefOrValue> attachments);

    /**
     * Deletes stored objects for attachments that were present in {@code existing} but are no
     * longer present in {@code updated}. Used during PATCH to clean up orphaned objects when
     * the client replaces or removes attachments.
     *
     * @param existing attachments currently persisted for the entity
     * @param updated  attachments supplied in the PATCH request
     * @return a {@code Mono} that completes when all orphaned deletions are done
     */
    Mono<Void> deleteOrphanedAttachments(List<AttachmentRefOrValue> existing, List<AttachmentRefOrValue> updated);
}
