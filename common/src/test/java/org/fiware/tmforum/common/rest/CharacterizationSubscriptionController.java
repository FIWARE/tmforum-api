package org.fiware.tmforum.common.rest;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.SubscriptionMapper;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.notification.NgsiLdEventHandler;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Exposes the {@link AbstractSubscriptionApiController} operations, callback included, for
 * {@link ErrorResponseCharacterizationTest}.
 */
@Requires(property = ScriptedBroker.ENABLED_PROPERTY, value = "true")
@Controller(CharacterizationSubscriptionController.BASE_PATH)
public class CharacterizationSubscriptionController extends AbstractSubscriptionApiController {

	public static final String BASE_PATH = "/characterization-hub";
	public static final String HUB_PATH = "/hub";
	private static final String EVENT_GROUP = EventConstants.EVENT_GROUP_CATALOG;

	public CharacterizationSubscriptionController(QueryParser queryParser,
			ReferenceValidationService validationService, EntityRepository repository,
			TMForumEventHandler tmForumEventHandler, NgsiLdEventHandler ngsiLdEventHandler,
			GeneralProperties generalProperties, EntityVOMapper entityVOMapper,
			SubscriptionMapper subscriptionMapper) {
		super(queryParser, validationService, repository, Map.of(EVENT_GROUP, CharacterizationEntity.TYPE),
				tmForumEventHandler, ngsiLdEventHandler, generalProperties, entityVOMapper, subscriptionMapper);
	}

	@Post(HUB_PATH)
	public Mono<HttpResponse<Object>> register(@QueryValue String callback) {
		return create(buildSubscription(callback, null, List.of(EVENT_GROUP)))
				.map(subscription -> HttpResponse.status(HttpStatus.CREATED));
	}

	@Delete(HUB_PATH + "/{id}")
	public Mono<HttpResponse<Object>> unregister(@PathVariable String id) {
		return delete(id);
	}
}
