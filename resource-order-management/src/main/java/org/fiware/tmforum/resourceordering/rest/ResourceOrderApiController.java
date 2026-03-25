package org.fiware.tmforum.resourceordering.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourceordering.api.ResourceOrderApi;
import org.fiware.resourceordering.model.ResourceOrderCreateVO;
import org.fiware.resourceordering.model.ResourceOrderUpdateVO;
import org.fiware.resourceordering.model.ResourceOrderVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resourceordering.TMForumMapper;
import org.fiware.tmforum.resourceordering.domain.ResourceOrder;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;

/**
 * Controller implementing the TMF652 Resource Order Management API endpoints for ResourceOrder.
 */
@Slf4j
@Controller("${api.resource-order-management.basepath:/}")
public class ResourceOrderApiController extends AbstractApiController<ResourceOrder> implements ResourceOrderApi {

	private final TMForumMapper tmForumMapper;
	private final Clock clock;

	public ResourceOrderApiController(QueryParser queryParser, ReferenceValidationService validationService,
									  TmForumRepository repository, TMForumMapper tmForumMapper, Clock clock, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
		this.tmForumMapper = tmForumMapper;
		this.clock = clock;
	}

	@Override
	public Mono<HttpResponse<ResourceOrderVO>> createResourceOrder(
			@NonNull ResourceOrderCreateVO resourceOrderCreateVO) {
		ResourceOrder resourceOrder = tmForumMapper.map(
				tmForumMapper.map(resourceOrderCreateVO,
						IdHelper.toNgsiLd(UUID.randomUUID().toString(), ResourceOrder.TYPE_RESOURCE_ORDER)));
		resourceOrder.setOrderDate(clock.instant());

		return create(getCheckingMono(resourceOrder), ResourceOrder.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::created);
	}

	@Override
	public Mono<HttpResponse<Object>> deleteResourceOrder(@NonNull String id) {
		return delete(id);
	}

	@Override
	public Mono<HttpResponse<List<ResourceOrderVO>>> listResourceOrder(@Nullable String fields,
																		@Nullable Integer offset,
																		@Nullable Integer limit) {
		return list(offset, limit, ResourceOrder.TYPE_RESOURCE_ORDER, ResourceOrder.class)
				.map(resourceOrderStream -> resourceOrderStream
						.map(tmForumMapper::map)
						.toList())
				.switchIfEmpty(Mono.just(List.of()))
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ResourceOrderVO>> patchResourceOrder(@NonNull String id,
																   @NonNull ResourceOrderUpdateVO resourceOrderUpdateVO) {
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such resource order cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}

		ResourceOrder resourceOrder = tmForumMapper.map(resourceOrderUpdateVO, id);

		if (Optional.ofNullable(resourceOrder.getOrderItem()).map(List::isEmpty).orElse(false)) {
			resourceOrder.setOrderItem(null);
		}

		return patch(id, resourceOrder, getCheckingMono(resourceOrder), ResourceOrder.class)
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	@Override
	public Mono<HttpResponse<ResourceOrderVO>> retrieveResourceOrder(@NonNull String id,
																	  @Nullable String fields) {
		return retrieve(id, ResourceOrder.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such resource order exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(tmForumMapper::map)
				.map(HttpResponse::ok);
	}

	private Mono<ResourceOrder> getCheckingMono(ResourceOrder resourceOrder) {
		List<List<? extends ReferencedEntity>> references = new ArrayList<>();
		references.add(resourceOrder.getRelatedParty());

		return getCheckingMono(resourceOrder, references)
				.onErrorMap(throwable ->
						new TmForumException(
								String.format("Was not able to create resource order %s", resourceOrder.getId()),
								throwable,
								TmForumExceptionReason.INVALID_RELATIONSHIP));
	}
}
