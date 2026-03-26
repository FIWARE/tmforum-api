package org.fiware.tmforum.serviceordering.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.serviceordering.api.CancelServiceOrderApi;
import org.fiware.serviceordering.model.CancelServiceOrderCreateVO;
import org.fiware.serviceordering.model.CancelServiceOrderVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.serviceordering.TMForumMapper;
import org.fiware.tmforum.serviceordering.domain.CancelServiceOrder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Controller implementing the TMF641 Service Order Management API endpoints for CancelServiceOrder.
 */
@Slf4j
@Controller("${api.service-order-management.basepath:/}")
public class CancelServiceOrderApiController extends AbstractApiController<CancelServiceOrder>
		implements CancelServiceOrderApi {

	private final TMForumMapper tmForumMapper;

	/**
	 * Creates the cancel service order API controller.
	 *
	 * @param queryParser       the query parser for NGSI-LD queries
	 * @param validationService the reference validation service
	 * @param repository        the TM Forum repository
	 * @param tmForumMapper     the mapper between VOs and domain objects
	 * @param eventHandler      the event handler for publishing domain events
	 */
	public CancelServiceOrderApiController(
			QueryParser queryParser,
			ReferenceValidationService validationService,
			TmForumRepository repository, TMForumMapper tmForumMapper, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<CancelServiceOrderVO>> createCancelServiceOrder(
			@NonNull CancelServiceOrderCreateVO cancelServiceOrderCreateVO) {
		if (cancelServiceOrderCreateVO.getServiceOrder() == null) {
			throw new TmForumException("Received a cancellation without a service order.",
					TmForumExceptionReason.INVALID_DATA);
		}
		CancelServiceOrder cancelServiceOrder = tmForumMapper.map(
				tmForumMapper.map(cancelServiceOrderCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), CancelServiceOrder.TYPE_CANCEL_SERVICE_ORDER)));

		Mono<CancelServiceOrder> checkingMono = getCheckingMono(cancelServiceOrder,
				List.of(List.of(cancelServiceOrder.getServiceOrder()))).onErrorMap(throwable ->
				new TmForumException(
						String.format("Was not able to cancel service order %s", cancelServiceOrder.getId()),
						throwable,
						TmForumExceptionReason.INVALID_RELATIONSHIP));

		return create(checkingMono,
				CancelServiceOrder.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<List<CancelServiceOrderVO>>> listCancelServiceOrder(@Nullable String fields,
			@Nullable Integer offset, @Nullable Integer limit) {
		return list(offset, limit, CancelServiceOrder.TYPE_CANCEL_SERVICE_ORDER, CancelServiceOrder.class)
				.map(cancelServiceOrderStream -> cancelServiceOrderStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<CancelServiceOrderVO>> retrieveCancelServiceOrder(@NonNull String id,
			@Nullable String fields) {
		return retrieve(id, CancelServiceOrder.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such cancel service order exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}
}
