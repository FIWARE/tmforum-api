package org.fiware.resourcecatalog.api;

import org.fiware.resourcecatalog.model.*;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
public interface EventsSubscriptionApi {

	java.lang.String PATH_REGISTER_LISTENER = "/hub";
	java.lang.String PATH_UNREGISTER_LISTENER = "/hub/{id}";

	@io.micronaut.http.annotation.Post("/hub")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> registerListener(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			EventSubscriptionInputVO data);

	@io.micronaut.http.annotation.Delete("/hub/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<Object>> unregisterListener(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);
}