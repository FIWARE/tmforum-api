package org.fiware.servicecatalog.api;

import org.fiware.servicecatalog.model.*;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
public interface ServiceSpecificationApi {

	java.lang.String PATH_CREATE_SERVICE_SPECIFICATION = "/serviceSpecification";
	java.lang.String PATH_DELETE_SERVICE_SPECIFICATION = "/serviceSpecification/{id}";
	java.lang.String PATH_LIST_SERVICE_SPECIFICATION = "/serviceSpecification";
	java.lang.String PATH_PATCH_SERVICE_SPECIFICATION = "/serviceSpecification/{id}";
	java.lang.String PATH_RETRIEVE_SERVICE_SPECIFICATION = "/serviceSpecification/{id}";

	@io.micronaut.http.annotation.Post("/serviceSpecification")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceSpecificationVO>> createServiceSpecification(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceSpecificationCreateVO serviceSpecification);

	@io.micronaut.http.annotation.Delete("/serviceSpecification/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<Object>> deleteServiceSpecification(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Get("/serviceSpecification")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<java.util.List<ServiceSpecificationVO>>> listServiceSpecification(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "offset")
			java.lang.Integer offset,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "limit")
			java.lang.Integer limit);

	@io.micronaut.http.annotation.Patch("/serviceSpecification/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceSpecificationVO>> patchServiceSpecification(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceSpecificationUpdateVO serviceSpecification);

	@io.micronaut.http.annotation.Get("/serviceSpecification/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceSpecificationVO>> retrieveServiceSpecification(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);
}