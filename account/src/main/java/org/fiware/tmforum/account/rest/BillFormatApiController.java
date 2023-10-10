package org.fiware.tmforum.account.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.account.api.BillFormatApi;
import org.fiware.account.api.BillFormatApi;
import org.fiware.account.model.BillFormatCreateVO;
import org.fiware.account.model.BillFormatUpdateVO;
import org.fiware.account.model.BillFormatVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.account.TMForumMapper;
import org.fiware.tmforum.account.domain.BillFormat;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class BillFormatApiController extends AbstractApiController<BillFormat> implements BillFormatApi {

    private final TMForumMapper tmForumMapper;

    public BillFormatApiController(ReferenceValidationService validationService,
                                TmForumRepository productBillFormatRepository, TMForumMapper tmForumMapper, EventHandler eventHandler) {
        super(validationService, productBillFormatRepository, eventHandler);
        this.tmForumMapper = tmForumMapper;
    }

    @Override
    public Mono<HttpResponse<BillFormatVO>> createBillFormat(BillFormatCreateVO billFormatVo) {
        BillFormat billFormat = tmForumMapper.map(
                tmForumMapper.map(billFormatVo, IdHelper.toNgsiLd(UUID.randomUUID().toString(), BillFormat.TYPE_BILLF)));

        return create(getCheckingMono(billFormat), BillFormat.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<BillFormat> getCheckingMono(BillFormat billFormat) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        return getCheckingMono(billFormat, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create billFormat %s", billFormat.getId()), throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteBillFormat(String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<BillFormatVO>>> listBillFormat(@Nullable String fields, @Nullable Integer offset,
                                                           @Nullable Integer limit) {
        return list(offset, limit, BillFormat.TYPE_BILLF, BillFormat.class)
                .map(categoryStream -> categoryStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<BillFormatVO>> patchBillFormat(String id, BillFormatUpdateVO billFormatUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException("Did not receive a valid id, such billFormat cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }
        BillFormat updatedBillFormat = tmForumMapper.map(tmForumMapper.map(billFormatUpdateVO, id));

        return patch(id, updatedBillFormat, getCheckingMono(updatedBillFormat), BillFormat.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<BillFormatVO>> retrieveBillFormat(String id, @Nullable String fields) {
        return retrieve(id, BillFormat.class)
                .switchIfEmpty(Mono.error(new TmForumException("No such billFormat exists.",
                        TmForumExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}

