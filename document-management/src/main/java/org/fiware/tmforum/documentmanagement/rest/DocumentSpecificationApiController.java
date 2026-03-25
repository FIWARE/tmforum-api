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
import org.fiware.tmforum.documentmanagement.TMForumMapper;
import org.fiware.tmforum.documentmanagement.domain.DocumentSpecification;
import org.fiware.tmforum.documentmanagement.s3.S3AttachmentService;
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
    private final S3AttachmentService s3AttachmentService;

    public DocumentSpecificationApiController(
            QueryParser queryParser,
            ReferenceValidationService validationService,
            TmForumRepository repository,
            TMForumMapper tmForumMapper,
            Clock clock,
            TMForumEventHandler eventHandler,
            @Nullable S3AttachmentService s3AttachmentService) {
        super(queryParser, validationService, repository, eventHandler);
        this.tmForumMapper = tmForumMapper;
        this.clock = clock;
        this.s3AttachmentService = s3AttachmentService;
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

        if (s3AttachmentService != null) {
            if (docSpec.getAttachment() != null) {
                docSpec.getAttachment().forEach(s3AttachmentService::validateAttachmentContent);
            }
            docSpec.setAttachment(
                    s3AttachmentService.offloadAttachments(docSpec.getAttachment(), docSpec.getId().toString()));
        } else if (docSpec.getAttachment() != null) {
            docSpec.getAttachment().stream()
                    .filter(att -> att.getContent() != null && !att.getContent().isEmpty())
                    .findFirst()
                    .ifPresent(att -> {
                        throw new TmForumException(
                                "Attachments with inline content are not supported when no S3 storage is configured. Provide a URL reference instead.",
                                TmForumExceptionReason.INVALID_DATA);
                    });
        }

        return create(getCheckingMono(docSpec), DocumentSpecification.class)
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
                .doOnNext(docSpec -> {
                    if (s3AttachmentService != null) {
                        s3AttachmentService.deleteAttachments(docSpec.getAttachment());
                    }
                })
                .flatMap(docSpec -> delete(id))
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
        // PATCH is not implemented per requirements (only GET/POST/DELETE)
        throw new TmForumException(
                "PATCH method is not supported for document specifications.",
                TmForumExceptionReason.INVALID_DATA);
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
                .map(docSpec -> {
                    if (s3AttachmentService != null) {
                        docSpec.setAttachment(s3AttachmentService.hydrateAttachments(docSpec.getAttachment()));
                    }
                    return docSpec;
                })
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    private Mono<DocumentSpecification> getCheckingMono(DocumentSpecification docSpec) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(docSpec.getRelatedParty());

        return getCheckingMono(docSpec, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create document specification %s", docSpec.getId()),
                        throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }
}
