package org.fiware.tmforum.documentmanagement.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.document.api.DocumentSpecificationApi;
import org.fiware.document.model.DocumentSpecificationCreateVO;
import org.fiware.document.model.DocumentSpecificationUpdateVO;
import org.fiware.document.model.DocumentSpecificationVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.documentmanagement.AttachmentService;
import org.fiware.tmforum.documentmanagement.TMForumMapper;
import org.fiware.tmforum.documentmanagement.domain.DocumentSpecification;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${api.document-management.basepath:/}")
public class DocumentSpecificationApiController extends AbstractApiController<DocumentSpecification>
        implements DocumentSpecificationApi {

    private final TMForumMapper tmForumMapper;
    private final Clock clock;
    @Nullable
    private final AttachmentService attachmentService;

    public DocumentSpecificationApiController(
            QueryParser queryParser,
            ReferenceValidationService validationService,
            TmForumRepository repository,
            TMForumMapper tmForumMapper,
            Clock clock,
            TMForumEventHandler eventHandler,
            @Nullable AttachmentService attachmentService) {
        super(queryParser, validationService, repository, eventHandler);
        this.tmForumMapper = tmForumMapper;
        this.clock = clock;
        this.attachmentService = attachmentService;
    }

    @Override
    public Mono<HttpResponse<DocumentSpecificationVO>> createDocumentSpecification(
            DocumentSpecificationCreateVO createVO) {

        if (createVO.getName() == null || createVO.getName().trim().isEmpty()) {
            throw new TmForumException(
                    "Name field is required and must not be blank to create a document specification.",
                    TmForumExceptionReason.INVALID_DATA);
        }

        DocumentSpecification docSpec = tmForumMapper.map(
                tmForumMapper.map(createVO,
                        IdHelper.toNgsiLd(UUID.randomUUID().toString(),
                                DocumentSpecification.TYPE_DOCUMENT_SPECIFICATION)));

        docSpec.setLastUpdate(clock.instant());

        List<AttachmentRefOrValue> attachments = docSpec.getAttachment();
        if (attachments == null || attachments.isEmpty()) {
            // nothing to validate
        } else if (attachmentService != null) {
            attachments.forEach(attachmentService::validateAttachmentContent);
        } else {
            attachments.stream()
                    .filter(att -> att.getContent() != null && !att.getContent().isEmpty())
                    .findFirst()
                    .ifPresent(att -> {
                        throw new TmForumException(
                                "Attachments with inline content are not supported when no AttachmentService is configured. Provide a URL reference instead.",
                                TmForumExceptionReason.INVALID_DATA);
                    });
        }

        Mono<DocumentSpecification> preparedSpec = attachmentService != null
                ? attachmentService.offloadAttachments(docSpec.getAttachment(), docSpec.getId().toString())
                        .doOnNext(docSpec::setAttachment)
                        .thenReturn(docSpec)
                : Mono.just(docSpec);

        return preparedSpec
                .flatMap(spec -> create(getCheckingMono(spec), DocumentSpecification.class))
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    @Override
    public Mono<HttpResponse<Object>> deleteDocumentSpecification(String id) {
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException(
                    "Did not receive a valid id, such document specification cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }

        // First retrieve to get attachment info for S3 cleanup
        return retrieve(id, DocumentSpecification.class)
                .flatMap(docSpec -> {
                    Mono<Void> deletionStep = attachmentService != null
                            ? attachmentService.deleteAttachments(docSpec.getAttachment())
                            : Mono.empty();
                    return deletionStep.then(delete(id));
                })
                .switchIfEmpty(Mono.defer(() -> delete(id)));
    }

    @Override
    public Mono<HttpResponse<List<DocumentSpecificationVO>>> listDocumentSpecification(
            @Nullable String fields,
            @Nullable Integer offset,
            @Nullable Integer limit) {
        // List returns retrieval info only (no hydration)
        return list(offset, limit, DocumentSpecification.TYPE_DOCUMENT_SPECIFICATION, DocumentSpecification.class)
                .map(stream -> stream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<DocumentSpecificationVO>> patchDocumentSpecification(
            String id,
            DocumentSpecificationUpdateVO updateVO) {
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException(
                    "Did not receive a valid id, such document specification cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }

        DocumentSpecification updatedSpec = tmForumMapper.map(updateVO, id);

        if (attachmentService == null) {
            return patch(id, updatedSpec, getCheckingMono(updatedSpec), DocumentSpecification.class)
                    .map(tmForumMapper::map)
                    .map(HttpResponse::ok);
        }

        return retrieve(id, DocumentSpecification.class)
                .flatMap(existing -> {
                    List<AttachmentRefOrValue> newAttachments = updatedSpec.getAttachment();
                    if (newAttachments == null || newAttachments.isEmpty()) {
                        return Mono.just(updatedSpec);
                    }
                    attachmentService.deleteOrphanedAttachments(existing.getAttachment(), newAttachments);
                    return attachmentService.offloadAttachments(newAttachments, id)
                            .doOnNext(updatedSpec::setAttachment)
                            .thenReturn(updatedSpec);
                })
                .switchIfEmpty(Mono.just(updatedSpec))
                .flatMap(spec -> patch(id, spec, getCheckingMono(spec), DocumentSpecification.class))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<DocumentSpecificationVO>> retrieveDocumentSpecification(
            String id,
            @Nullable String fields) {
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException(
                    "Did not receive a valid id, such document specification cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }

        return retrieve(id, DocumentSpecification.class)
                .switchIfEmpty(Mono.error(new TmForumException(
                        "No such document specification exists.",
                        TmForumExceptionReason.NOT_FOUND)))
                .flatMap(docSpec -> {
                    if (attachmentService != null) {
                        return attachmentService.resolveAttachments(docSpec.getAttachment())
                                .doOnNext(docSpec::setAttachment)
                                .thenReturn(docSpec);
                    }
                    return Mono.just(docSpec);
                })
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    private Mono<DocumentSpecification> getCheckingMono(DocumentSpecification docSpec) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(docSpec.getRelatedParty());
        references.add(docSpec.getConstraint());
        references.add(docSpec.getEntitySpecRelationship());

        return getCheckingMono(docSpec, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create document specification %s", docSpec.getId()),
                        throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }
}
