package org.fiware.tmforum.common.rest;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.context.ServerRequestContext;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.filter.PaginationFilter;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.EntityEventPublisher;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.ParsedQuery;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;
import static org.fiware.tmforum.common.CommonConstants.MAX_LIMIT;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApiController<T> {

	private static final String TYPE_SEPARATOR = ",";

	protected final QueryParser queryParser;
	protected final ReferenceValidationService validationService;
	protected final EntityRepository repository;
	protected final TMForumEventHandler eventHandler;
	protected EntityEventPublisher eventPublisher;

	/** Setter-injected so the module constructors stay unchanged. */
	@Inject
	public void setEventPublisher(EntityEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	protected Mono<T> getCheckingMono(T entityToCheck, List<List<? extends ReferencedEntity>> referencedEntities) {
		Mono<T> checkingMono = Mono.just(entityToCheck);
		for (List<? extends ReferencedEntity> referencedEntitiesList : referencedEntities) {
			if (referencedEntitiesList != null && !referencedEntitiesList.isEmpty()) {
				checkingMono = Mono.zip(checkingMono,
						validationService.getCheckingMono(referencedEntitiesList, entityToCheck), (p1, p2) -> p1);
			}
		}
		return checkingMono;
	}

	protected Mono<T> create(Mono<T> checkingMono, Class<T> entityClass) {
		return checkingMono
				.flatMap(checkedResult -> eventPublisher.createWithEvent(checkedResult)
						.then(Mono.just(checkedResult)))
				.onErrorMap(AbstractApiController::isRejectedWrite, t -> {
					StorageException e = (StorageException) t;
					log.warn("Broker rejected the entity creation with reason {}: {}", e.reason(), e.getMessage());
					return rejectedWrite(e, "creating");
				})
				.cast(entityClass);
	}

	/**
	 * Deletes the entity the id names, if its type is one the endpoint serves.
	 *
	 * @param acceptedTypes the comma-separated NGSI-LD types this endpoint owns, the same ones it lists; an id whose
	 *                      type segment is none of them names an entity of another endpoint and is answered with 404,
	 *                      so that DELETE, GET and PATCH agree on the same URL
	 * @param id            the id of the entity to delete
	 * @return 204, once the entity and its event are committed
	 * @throws TmForumException if the id is not an NGSI-LD id or carries a type segment of another endpoint
	 */
	protected Mono<HttpResponse<Object>> delete(String id, String acceptedTypes) {
		List<String> types = acceptedTypes(acceptedTypes);
		return eventPublisher.deleteWithEvent(ownedEntityId(id, types), types)
				.then(Mono.just(HttpResponse.noContent()));
	}

	/**
	 * The id of an entity this endpoint could hold.
	 *
	 * @param id    the received id
	 * @param types the NGSI-LD types this endpoint serves
	 * @return the id as a URI
	 * @throws TmForumException if the id is not an NGSI-LD id or its type segment is not one of {@code types}
	 */
	protected static URI ownedEntityId(String id, List<String> types) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such entity cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		URI entityId = URI.create(id);
		// an id of another type names an entity of another endpoint, which this one must not touch
		if (IdHelper.typeSegment(entityId).filter(types::contains).isEmpty()) {
			throw new TmForumException("Did not receive a valid id, such entity cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		return entityId;
	}

	/** The individual NGSI-LD types of a comma-separated list, as the list endpoints take them. */
	protected static List<String> acceptedTypes(String commaSeparatedTypes) {
		return Arrays.stream(Optional.ofNullable(commaSeparatedTypes).orElse("").split(TYPE_SEPARATOR))
				.map(String::trim)
				.filter(type -> !type.isEmpty())
				.toList();
	}

	/**
	 * Rejects {@code offset}/{@code limit} values that are not integers.
	 * <p>
	 * Both are declared as {@code @Nullable Integer} on the generated api interfaces, so Micronaut
	 * binds {@code null} when the received value cannot be converted to one. By the time the request
	 * reaches the controller, an invalid value is therefore indistinguishable from an absent one and
	 * would be answered with the default page instead of being reported - hence the raw values are
	 * taken from the request itself.
	 * </p>
	 *
	 * @param request the request to validate the pagination parameters of
	 * @throws TmForumException if one of the parameters was provided with a non-integer value
	 */
	private void validatePaginationParameters(HttpRequest<Object> request) {
		Stream.of(QueryParser.OFFSET_KEY, QueryParser.LIMIT_KEY)
				.forEach(parameterName -> request.getParameters()
						.getAll(parameterName)
						.forEach(value -> validateIntegerParameter(parameterName, value)));
	}

	private static void validateIntegerParameter(String parameterName, String value) {
		try {
			Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			throw new TmForumException(String.format("%s is not a valid value for %s.", value, parameterName),
					TmForumExceptionReason.INVALID_DATA);
		}
	}

	// the storage arguments of a list call; a null query means unfiltered and unordered
	private record ListRequest(EntityQuery query, String ids, String types) {
	}

	private ListRequest toListRequest(Optional<HttpRequest<Object>> optionalHttpRequest, Class<?> queryClass,
			String defaultTypes) {
		if (optionalHttpRequest.isEmpty()) {
			log.warn("The original request is not available, no filters will be applied.");
			return new ListRequest(null, null, defaultTypes);
		}
		HttpRequest<Object> theRequest = optionalHttpRequest.get();
		validatePaginationParameters(theRequest);
		Map<String, List<String>> parameters = theRequest.getParameters().asMap();
		// resolve the sort before hasFilter, which mutates parameters by removing well-known keys (incl. sort)
		List<SortKey> sort = queryParser.parseSort(queryClass, parameters);
		ParsedQuery parsedQuery = null;
		if (QueryParser.hasFilter(parameters)) {
			log.debug("A filter is included in the request.");
			parsedQuery = queryParser.parse(queryClass, theRequest.getUri().getQuery());
		}
		QueryFilter filter = parsedQuery == null ? null : parsedQuery.filter();
		EntityQuery query = filter == null && sort.isEmpty() ? null : new EntityQuery(queryClass, filter, sort);
		if (parsedQuery == null) {
			return new ListRequest(query, null, defaultTypes);
		}
		return new ListRequest(query, parsedQuery.ids(), Optional.ofNullable(parsedQuery.types()).orElse(defaultTypes));
	}


	/**
	 * Applies the default and the upper bound to a requested page size.
	 * <p>
	 * An unbounded limit would make the storage read, count and map every stored entity of the requested types in
	 * one request, so a larger value is clamped to {@link org.fiware.tmforum.common.CommonConstants#MAX_LIMIT}
	 * instead of being served. The applied value is reported back through the pagination headers.
	 * </p>
	 *
	 * @param limit the requested limit, null when the request carries none
	 * @return the limit to query the storage with
	 * @throws TmForumException if the requested limit is below one
	 */
	private static int boundedLimit(Integer limit) {
		int requestedLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
		if (requestedLimit < 1) {
			throw new TmForumException(String.format("Invalid limit %s.", requestedLimit),
					TmForumExceptionReason.INVALID_DATA);
		}
		if (requestedLimit > MAX_LIMIT) {
			log.debug("The requested limit {} exceeds the maximum of {}, the page is limited to it.", requestedLimit,
					MAX_LIMIT);
			return MAX_LIMIT;
		}
		return requestedLimit;
	}

	private static int validOffset(Integer offset) {
		int requestedOffset = Optional.ofNullable(offset).orElse(DEFAULT_OFFSET);
		if (requestedOffset < 0) {
			throw new TmForumException(String.format("Invalid offset %s.", requestedOffset),
					TmForumExceptionReason.INVALID_DATA);
		}
		return requestedOffset;
	}

	protected <R> Mono<Stream<R>> list(Integer offset, Integer limit, String type, Class<R> entityClass) {

		Optional<HttpRequest<Object>> optionalHttpRequest = ServerRequestContext.currentRequest();
		ListRequest listRequest = toListRequest(optionalHttpRequest, entityClass, type);
		offset = validOffset(offset);
		limit = boundedLimit(limit);

		return repository
				.findEntities(offset, limit, entityClass, listRequest.query(), listRequest.ids(), listRequest.types())
				.doOnNext(pagedResult -> optionalHttpRequest.ifPresent(theRequest -> {
					theRequest.setAttribute(PaginationFilter.OFFSET_ATTR, pagedResult.offset())
							.setAttribute(PaginationFilter.LIMIT_ATTR, pagedResult.limit())
							.setAttribute(PaginationFilter.RETURNED_COUNT_ATTR, pagedResult.items().size());
					if (pagedResult.totalCount() != null) {
						theRequest.setAttribute(PaginationFilter.TOTAL_COUNT_ATTR, pagedResult.totalCount());
					}
				}))
				.map(PagedResult::items)
				.map(List::stream);
	}

	/**
	 * List entities of several NGSI-LD types at once (e.g. a base type and all its registered
	 * sub-types), in a single broker call, so that offset/limit/count are correct against the
	 * combined result set instead of being computed by fanning out one query per type and merging
	 * paginated slices client-side.
	 *
	 * @param offset      requested offset
	 * @param limit       requested limit
	 * @param types       comma-separated list of NGSI-LD types to query (OR semantics)
	 * @param queryClass  class used to resolve/validate filter attributes from the request's query string
	 * @param typeToClass resolves the domain class to map an entity to, based on its NGSI-LD type
	 */
	protected <R> Mono<Stream<R>> listPolymorphic(Integer offset, Integer limit, String types,
			Class<R> queryClass, Function<String, Class<? extends R>> typeToClass) {

		Optional<HttpRequest<Object>> optionalHttpRequest = ServerRequestContext.currentRequest();
		ListRequest listRequest = toListRequest(optionalHttpRequest, queryClass, types);
		offset = validOffset(offset);
		limit = boundedLimit(limit);

		return repository
				.findEntitiesPolymorphic(offset, limit, listRequest.types(), listRequest.query(), listRequest.ids(),
						typeToClass)
				.doOnNext(pagedResult -> optionalHttpRequest.ifPresent(theRequest -> {
					theRequest.setAttribute(PaginationFilter.OFFSET_ATTR, pagedResult.offset())
							.setAttribute(PaginationFilter.LIMIT_ATTR, pagedResult.limit())
							.setAttribute(PaginationFilter.RETURNED_COUNT_ATTR, pagedResult.items().size());
					if (pagedResult.totalCount() != null) {
						theRequest.setAttribute(PaginationFilter.TOTAL_COUNT_ATTR, pagedResult.totalCount());
					}
				}))
				.map(PagedResult::items)
				.map(List::stream);
	}

	protected <R> Mono<R> retrieve(String id, Class<R> entityClass) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such entity cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		return repository
				.get(URI.create(id), entityClass);
	}

	/**
	 * Updates an existing entity: 404 before the checking mono runs, then the update with its event.
	 * Generic in the entity class, so that sub-type patches (TMF730) take the same path.
	 */
	protected <S extends T> Mono<S> patch(String id, S updatedObject, Mono<?> checkingMono, Class<S> entityClass) {
		return repository
				.get(URI.create(id), entityClass)
				.switchIfEmpty(
						Mono.error(new TmForumException("No such entity exists.", TmForumExceptionReason.NOT_FOUND)))
				.flatMap(entity -> checkingMono)
				.flatMap(checked -> eventPublisher.updateWithEvent(id, updatedObject, entityClass))
				.onErrorMap(AbstractApiController::isRejectedWrite, t -> rejectedWrite((StorageException) t, "updating"));
	}

	// the backend answered a write with an error; every other failure is rendered by the exception handlers
	private static boolean isRejectedWrite(Throwable t) {
		return t instanceof StorageException e
				&& (e.operation() == StorageOperation.CREATE || e.operation() == StorageOperation.UPDATE)
				&& switch (e.reason()) {
					case CONFLICT, INVALID_DATA, NOT_FOUND, UNKNOWN -> true;
					default -> false;
				};
	}

	private static TmForumException rejectedWrite(StorageException e, String action) {
		return switch (e.reason()) {
			case CONFLICT -> new TmForumException(
					String.format("Conflict on %s the entity: %s", action, e.getMessage()),
					TmForumExceptionReason.CONFLICT);
			case INVALID_DATA -> new TmForumException(
					String.format("Did not receive a valid entity: %s.", e.getMessage()),
					TmForumExceptionReason.INVALID_DATA);
			default -> new TmForumException(
					String.format("Unspecified downstream error: %s", e.getMessage()),
					TmForumExceptionReason.UNKNOWN);
		};
	}

	/**
	 * Writes the related entities through the repository, without TMForum events. No production controller uses it.
	 */
	protected <R extends EntityWithId> Mono<T> relatedEntityHandlingMono(T entity, Mono<T> entityMono,
																		 List<R> relatedList, Consumer<List<R>> entityUpdater, Class<R> relatedEntityClass) {
		List<R> relatedEntities = Optional.ofNullable(relatedList).orElseGet(List::of);
		if (!relatedEntities.isEmpty()) {
			Mono<List<R>> relatedEntitiesMono = Mono.zip(
					relatedEntities
							.stream()
							.map(re ->
									repository
											.updateDomainEntity(re.getId().toString(), re)
											.onErrorResume(t -> repository.createDomainEntity(re))
											.then(Mono.just(re))
							)
							.toList(),
					t -> Arrays.stream(t).map(relatedEntityClass::cast).toList());

			Mono<T> updatingMono = relatedEntitiesMono
					.map(updatedRelatedEntity -> {
						entityUpdater.accept(updatedRelatedEntity);
						return entity;
					});
			entityMono = Mono.zip(entityMono, updatingMono, (e1, e2) -> e1);
		}
		return entityMono;
	}

}
