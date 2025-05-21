package org.fiware.tmforum.common.rest;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.context.ServerRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParams;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApiController<T> {

	protected final QueryParser queryParser;
	protected final ReferenceValidationService validationService;
	protected final TmForumRepository repository;
	protected final TMForumEventHandler eventHandler;

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
				.flatMap(checkedResult ->
						repository.createDomainEntity(checkedResult)
								.doOnSuccess(v -> {
									eventHandler.handleCreateEvent(checkedResult)
											.doOnError(e -> log.warn("Was not able to handle the create event.", e))
											.subscribe();
								})
								.then(Mono.just(checkedResult)))
				.onErrorMap(t -> {
					if (t instanceof HttpClientResponseException e) {
						return switch (e.getStatus()) {
							case CONFLICT -> new TmForumException(
									String.format("Conflict on creating the entity: %s", e.getMessage()),
									TmForumExceptionReason.CONFLICT);
							case BAD_REQUEST -> new TmForumException(
									String.format("Did not receive a valid entity: %s.", e.getMessage()),
									TmForumExceptionReason.INVALID_DATA);
							default -> new TmForumException(
									String.format("Unspecified downstream error: %s", e.getMessage()),
									TmForumExceptionReason.UNKNOWN);
						};
					} else {
						return t;
					}
				})
				.cast(entityClass);
	}

	protected Mono<HttpResponse<Object>> delete(String id) {
		// non-ngsi-ld ids cannot exist.
		if (!IdHelper.isNgsiLdId(id)) {
			throw new TmForumException("Did not receive a valid id, such entity cannot exist.",
					TmForumExceptionReason.NOT_FOUND);
		}
		return repository.deleteDomainEntity(URI.create(id))
				.then(Mono.just(HttpResponse.noContent()));
	}

	protected <R> Mono<Stream<R>> list(Integer offset, Integer limit, String type, Class<R> entityClass) {

		Optional<HttpRequest<Object>> optionalHttpRequest = ServerRequestContext.currentRequest();
		QueryParams queryParams = null;
		if (optionalHttpRequest.isEmpty()) {
			log.warn("The original request is not available, no filters will be applied.");
		} else {
			HttpRequest<Object> theRequest = optionalHttpRequest.get();
			Map<String, List<String>> parameters = theRequest.getParameters().asMap();
			if (QueryParser.hasFilter(parameters)) {
				log.debug("A filter is included in the request.");
				String queryString = theRequest.getUri().getQuery();
				queryParams = queryParser.toNgsiLdQuery(entityClass, queryString);
			}
		}
		offset = Optional.ofNullable(offset).orElse(DEFAULT_OFFSET);
		limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);

		if (offset < 0 || limit < 1) {
			throw new TmForumException(String.format("Invalid offset %s or limit %s.", offset, limit),
					TmForumExceptionReason.INVALID_DATA);
		}

		return repository
				.findEntities(offset, limit, entityClass,
						Optional.ofNullable(queryParams).map(QueryParams::query).orElse(null),
						Optional.ofNullable(queryParams).map(QueryParams::id).orElse(null),
						Optional.ofNullable(queryParams).map(QueryParams::type).orElse(type))
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

	protected Mono<T> patch(String id, T updatedObject, Mono<T> checkingMono, Class<T> entityClass) {
		URI idUri = URI.create(id);

		AtomicReference<T> old = new AtomicReference<>();
		return repository
				.get(idUri, entityClass)
				.switchIfEmpty(
						Mono.error(new TmForumException("No such entity exists.", TmForumExceptionReason.NOT_FOUND)))
				.flatMap(entity -> {
					old.set(entity);
					return checkingMono;
				})
				.flatMap(entity -> repository.updateDomainEntity(id, updatedObject)
						.then(repository.get(idUri, entityClass))
						.doOnNext(updatedState ->
								eventHandler.handleUpdateEvent(updatedState, old.get())
										.doOnError(e -> log.warn("Was not able to handle the create event.", e))
										.subscribe()
						)
				);
	}

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
