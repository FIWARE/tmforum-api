package org.fiware.servicecatalog.api;

import org.fiware.servicecatalog.model.*;

/** Test client for {@link EventsSubscriptionApi}. **/
@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.http.client.annotation.Client("/")
public interface EventsSubscriptionApiTestClient {

	@io.micronaut.http.annotation.Post("/hub")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> registerListener(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			EventSubscriptionInputVO data);

	@io.micronaut.http.annotation.Post("/hub")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> registerListener(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			EventSubscriptionInputVO data);
	@io.micronaut.http.annotation.Delete("/hub/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<?> unregisterListener(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Delete("/hub/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<?> unregisterListener(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);}
