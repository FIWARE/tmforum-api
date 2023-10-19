package org.fiware.tmforum.account.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.account.api.BillPresentationMediaApi;
import org.fiware.account.model.BillPresentationMediaCreateVO;
import org.fiware.account.model.BillPresentationMediaUpdateVO;
import org.fiware.account.model.BillPresentationMediaVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.account.TMForumMapper;
import org.fiware.tmforum.account.domain.BillPresentationMedia;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class BillPresentationMediaApiController extends AbstractApiController<BillPresentationMedia> implements BillPresentationMediaApi {

    private final TMForumMapper tmForumMapper;

    public BillPresentationMediaApiController(QueryParser queryParser, ReferenceValidationService validationService,
                                              TmForumRepository productBillPresentationMediaRepository, TMForumMapper tmForumMapper, EventHandler eventHandler) {
        super(queryParser, validationService, productBillPresentationMediaRepository, eventHandler);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<BillPresentationMediaVO>> createBillPresentationMedia(BillPresentationMediaCreateVO billPresentationMediaVo) {
        BillPresentationMedia billPresentationMedia = tmForumMapper.map(
                tmForumMapper.map(billPresentationMediaVo, IdHelper.toNgsiLd(UUID.randomUUID().toString(), BillPresentationMedia.TYPE_BILLPM)));

        return create(getCheckingMono(billPresentationMedia), BillPresentationMedia.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<BillPresentationMedia> getCheckingMono(BillPresentationMedia billPresentationMedia) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        return getCheckingMono(billPresentationMedia, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create billPresentationMedia %s", billPresentationMedia.getId()), throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteBillPresentationMedia(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<BillPresentationMediaVO>>> listBillPresentationMedia(@Nullable String fields, @Nullable Integer offset,
                                                                                               @Nullable Integer limit) {
        return list(offset, limit, BillPresentationMedia.TYPE_BILLPM, BillPresentationMedia.class)
                .map(categoryStream -> categoryStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<BillPresentationMediaVO>> patchBillPresentationMedia(String id, BillPresentationMediaUpdateVO billPresentationMediaUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException("Did not receive a valid id, such billPresentationMedia cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }
        BillPresentationMedia updatedBillPresentationMedia = tmForumMapper.map(tmForumMapper.map(billPresentationMediaUpdateVO, id));

        return patch(id, updatedBillPresentationMedia, getCheckingMono(updatedBillPresentationMedia), BillPresentationMedia.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<BillPresentationMediaVO>> retrieveBillPresentationMedia(String id, @Nullable String fields) {
        return retrieve(id, BillPresentationMedia.class)
                .switchIfEmpty(Mono.error(new TmForumException("No such billPresentationMedia exists.",
                        TmForumExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}

