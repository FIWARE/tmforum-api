package org.fiware.tmforum.productordering.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.productordering.api.CancelProductOrderApi;
import org.fiware.productordering.model.CancelProductOrderCreateVO;
import org.fiware.productordering.model.CancelProductOrderVO;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.productordering.TMForumMapper;
import org.fiware.tmforum.productordering.domain.CancelProductOrder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class CancelProductOrderApiController extends AbstractApiController<CancelProductOrder>
		implements CancelProductOrderApi {

	private final TMForumMapper tmForumMapper;

	public CancelProductOrderApiController(
			ReferenceValidationService validationService,
			TmForumRepository repository, TMForumMapper tmForumMapper, EventHandler eventHandler) {
		super(validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<CancelProductOrderVO>> createCancelProductOrder(
			@NonNull CancelProductOrderCreateVO cancelProductOrderCreateVO) {
		if (cancelProductOrderCreateVO.getProductOrder() == null) {
			throw new TmForumException("Received a cancellation without a product.",
					TmForumExceptionReason.INVALID_DATA);
		}
		CancelProductOrder cancelProductOrder = tmForumMapper.map(
				tmForumMapper.map(cancelProductOrderCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), CancelProductOrder.TYPE_CANCEL_PRODUCT_ORDER)));

		Mono<CancelProductOrder> checkingMono = getCheckingMono(cancelProductOrder,
				List.of(List.of(cancelProductOrder.getProductOrder()))).onErrorMap(throwable ->
				new TmForumException(
						String.format("Was not able to cancel product order %s", cancelProductOrder.getId()),
						throwable,
						TmForumExceptionReason.INVALID_RELATIONSHIP));

		return create(checkingMono,
				CancelProductOrder.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	@Override public Mono<HttpResponse<List<CancelProductOrderVO>>> listCancelProductOrder(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, CancelProductOrder.TYPE_CANCEL_PRODUCT_ORDER, CancelProductOrder.class)
				.map(cancelProductOrderStream -> cancelProductOrderStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override public Mono<HttpResponse<CancelProductOrderVO>> retrieveCancelProductOrder(@NonNull String id,
			@Nullable String fields) {
		return retrieve(id, CancelProductOrder.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such cancel product order exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
