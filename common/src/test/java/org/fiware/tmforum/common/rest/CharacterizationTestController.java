package org.fiware.tmforum.common.rest;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Exposes the {@link AbstractApiController} operations for {@link ErrorResponseCharacterizationTest}. The optional
 * {@code reference} is validated, the optional {@code related} entity is written by the related-entity handling.
 */
@Requires(property = ScriptedBroker.ENABLED_PROPERTY, value = "true")
@Controller(CharacterizationTestController.BASE_PATH)
public class CharacterizationTestController extends AbstractApiController<CharacterizationEntity> {

	public static final String BASE_PATH = "/characterization";
	public static final String ENTITIES_PATH = "/entities";
	public static final String DIRECT_PATH = "/direct";
	private static final String NAME = "updated";

	public CharacterizationTestController(QueryParser queryParser, ReferenceValidationService validationService,
			TmForumRepository repository, TMForumEventHandler eventHandler) {
		super(queryParser, validationService, repository, eventHandler);
	}

	@Post(ENTITIES_PATH)
	public Mono<HttpResponse<Object>> createEntity(@Nullable @QueryValue String reference,
			@Nullable @QueryValue String related) {
		CharacterizationEntity entity = entity(UUID.randomUUID().toString(), related);
		return create(checkingMono(entity, reference, related), CharacterizationEntity.class)
				.map(created -> HttpResponse.status(HttpStatus.CREATED));
	}

	@Patch(ENTITIES_PATH + "/{id}")
	public Mono<HttpResponse<Object>> patchEntity(@PathVariable String id, @Nullable @QueryValue String reference,
			@Nullable @QueryValue String related) {
		CharacterizationEntity entity = entity(id, related);
		return patch(id, entity, checkingMono(entity, reference, related), CharacterizationEntity.class)
				.map(updated -> HttpResponse.ok());
	}

	@Delete(ENTITIES_PATH + "/{id}")
	public Mono<HttpResponse<Object>> deleteEntity(@PathVariable String id) {
		return delete(id);
	}

	@Get(ENTITIES_PATH)
	public Mono<HttpResponse<Object>> listEntities() {
		return list(null, null, CharacterizationEntity.TYPE, CharacterizationEntity.class)
				.map(entities -> HttpResponse.ok());
	}

	@Get(ENTITIES_PATH + "/{id}")
	public Mono<HttpResponse<Object>> retrieveEntity(@PathVariable String id) {
		return retrieve(id, CharacterizationEntity.class)
				.switchIfEmpty(Mono.error(new TmForumException("No such entity exists.",
						TmForumExceptionReason.NOT_FOUND)))
				.map(entity -> HttpResponse.ok());
	}

	/** Updates without the call-site error mapping, like the module controllers that call the repository. */
	@Patch(DIRECT_PATH + "/{id}")
	public Mono<HttpResponse<Object>> updateDirectly(@PathVariable String id) {
		return repository.updateDomainEntity(id, entity(id, null))
				.then(Mono.just(HttpResponse.ok()));
	}

	private static CharacterizationEntity entity(String id, @Nullable String related) {
		CharacterizationEntity entity = new CharacterizationEntity(id);
		entity.setName(NAME);
		Optional.ofNullable(related).ifPresent(relatedId -> entity.setRelated(List.of(new CharacterizationRelated(relatedId))));
		return entity;
	}

	private Mono<CharacterizationEntity> checkingMono(CharacterizationEntity entity, @Nullable String reference,
			@Nullable String related) {
		List<ReferencedEntity> references = Optional.ofNullable(reference)
				.<ReferencedEntity>map(id -> new CharacterizationReference(URI.create(id)))
				.map(List::of)
				.orElse(List.of());
		Mono<CharacterizationEntity> checkingMono = getCheckingMono(entity, List.of(references));
		return relatedEntityHandlingMono(entity, checkingMono, entity.getRelated(), entity::setRelated,
				CharacterizationRelated.class);
	}
}
