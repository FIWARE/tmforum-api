package org.fiware.tmforum.serviceordering.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.serviceordering.api.ServiceOrderApi;
import org.fiware.serviceordering.model.ServiceOrderCreateVO;
import org.fiware.serviceordering.model.ServiceOrderUpdateVO;
import org.fiware.serviceordering.model.ServiceOrderVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.serviceordering.TMForumMapper;
import org.fiware.tmforum.serviceordering.domain.ServiceOrder;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;

/**
 * Controller implementing the TMF641 Service Order Management API endpoints for ServiceOrder.
 */
@Slf4j
@Controller("${api.service-order-management.basepath:/}")
public class ServiceOrderApiController extends AbstractApiController<ServiceOrder> implements ServiceOrderApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	/**
	 * Creates the service order API controller.
	 *
	 * @param queryParser       the query parser for NGSI-LD queries
	 * @param validationService the reference validation service
	 * @param repository        the TM Forum repository
	 * @param tmForumMapper     the mapper between VOs and domain objects
	 * @param clock             the clock for setting order dates
	 * @param eventHandler      the event handler for publishing domain events
	 */
	public ServiceOrderApiController(QueryParser queryParser, ReferenceValidationService validationService,
									 TmForumRepository repository, TMForumMapper tmForumMapper, Clock clock, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<ServiceOrderVO>> createServiceOrder(
			@NonNull ServiceOrderCreateVO serviceOrderCreateVO) {
		ServiceOrder serviceOrder = tmForumMapper.map(
				tmForumMapper.map(serviceOrderCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), ServiceOrder.TYPE_SERVICE_ORDER)));
		serviceOrder.setOrderDate(clock.instant());

		return create(getCheckingMono(serviceOrder), ServiceOrder.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<Object>> deleteServiceOrder(@NonNull String id) {
		return delete(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<List<ServiceOrderVO>>> listServiceOrder(@Nullable String fields,
																	 @Nullable Integer offset,
																	 @Nullable Integer limit) {
		return list(offset, limit, ServiceOrder.TYPE_SERVICE_ORDER, ServiceOrder.class)
				.map(serviceOrderStream -> serviceOrderStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<ServiceOrderVO>> patchServiceOrder(@NonNull String id,
																@NonNull ServiceOrderUpdateVO serviceOrderUpdateVO) {
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such service order cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		ServiceOrder serviceOrder = tmForumMapper.map(serviceOrderUpdateVO, id);

		if (Optional.ofNullable(serviceOrder.getServiceOrderItem()).map(List::isEmpty).orElse(false)) {
			serviceOrder.setServiceOrderItem(null);
		}

		return patch(id, serviceOrder, getCheckingMono(serviceOrder), ServiceOrder.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mono<HttpResponse<ServiceOrderVO>> retrieveServiceOrder(@NonNull String id,
																   @Nullable String fields) {
		return retrieve(id, ServiceOrder.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such service order exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	/**
	 * Builds a checking mono that validates all references within the given service order.
	 *
	 * @param serviceOrder the service order whose references to validate
	 * @return a mono that emits the service order after successful validation
	 */
	private Mono<ServiceOrder> getCheckingMono(ServiceOrder serviceOrder) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(serviceOrder.getRelatedParty());

		return getCheckingMono(serviceOrder, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create service order %s", serviceOrder.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}
}
