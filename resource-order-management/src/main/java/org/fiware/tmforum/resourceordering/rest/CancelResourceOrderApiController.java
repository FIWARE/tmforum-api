package org.fiware.tmforum.resourceordering.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourceordering.api.CancelResourceOrderApi;
import org.fiware.resourceordering.model.CancelResourceOrderCreateVO;
import org.fiware.resourceordering.model.CancelResourceOrderVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.resourceordering.TMForumMapper;
import org.fiware.tmforum.resourceordering.domain.CancelResourceOrder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Controller implementing the TMF652 Resource Order Management API endpoints for CancelResourceOrder.
 */
@Slf4j
@Controller("${api.resource-order-management.basepath:/}")
public class CancelResourceOrderApiController extends AbstractApiController<CancelResourceOrder>
		implements CancelResourceOrderApi {

	private final TMForumMapper tmForumMapper;

	public CancelResourceOrderApiController(
			QueryParser queryParser,
			ReferenceValidationService validationService,
			TmForumRepository repository, TMForumMapper tmForumMapper, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	@Override
	public Mono<HttpResponse<CancelResourceOrderVO>> createCancelResourceOrder(
			@NonNull CancelResourceOrderCreateVO cancelResourceOrderCreateVO) {
		if (cancelResourceOrderCreateVO.getResourceOrder() == null) {
			throw new TmForumException("Received a cancellation without a resource order.",
					TmForumExceptionReason.INVALID_DATA);
		}
		CancelResourceOrder cancelResourceOrder = tmForumMapper.map(
				tmForumMapper.map(cancelResourceOrderCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), CancelResourceOrder.TYPE_CANCEL_RESOURCE_ORDER)));

		Mono<CancelResourceOrder> checkingMono = getCheckingMono(cancelResourceOrder,
				List.of(List.of(cancelResourceOrder.getResourceOrder()))).onErrorMap(throwable ->
				new TmForumException(
						String.format("Was not able to cancel resource order %s", cancelResourceOrder.getId()),
						throwable,
						TmForumExceptionReason.INVALID_RELATIONSHIP));

		return create(checkingMono,
				CancelResourceOrder.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	@Override
	public Mono<HttpResponse<List<CancelResourceOrderVO>>> listCancelResourceOrder(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, CancelResourceOrder.TYPE_CANCEL_RESOURCE_ORDER, CancelResourceOrder.class)
				.map(cancelResourceOrderStream -> cancelResourceOrderStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<CancelResourceOrderVO>> retrieveCancelResourceOrder(@NonNull String id,
			@Nullable String fields) {
		return retrieve(id, CancelResourceOrder.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such cancel resource order exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
