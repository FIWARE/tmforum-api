package org.fiware.resourcecatalog.api;

import org.fiware.resourcecatalog.model.*;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
public interface ResourceSpecificationApi {

	java.lang.String PATH_CREATE_RESOURCE_SPECIFICATION = "/resourceSpecification";
	java.lang.String PATH_DELETE_RESOURCE_SPECIFICATION = "/resourceSpecification/{id}";
	java.lang.String PATH_LIST_RESOURCE_SPECIFICATION = "/resourceSpecification";
	java.lang.String PATH_PATCH_RESOURCE_SPECIFICATION = "/resourceSpecification/{id}";
	java.lang.String PATH_RETRIEVE_RESOURCE_SPECIFICATION = "/resourceSpecification/{id}";

	@io.micronaut.http.annotation.Post("/resourceSpecification")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ResourceSpecificationVO>> createResourceSpecification(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceSpecificationCreateVO resourceSpecification);

	@io.micronaut.http.annotation.Delete("/resourceSpecification/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<Object>> deleteResourceSpecification(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Get("/resourceSpecification")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<java.util.List<ResourceSpecificationVO>>> listResourceSpecification(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "offset")
			java.lang.Integer offset,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "limit")
			java.lang.Integer limit);

	@io.micronaut.http.annotation.Patch("/resourceSpecification/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ResourceSpecificationVO>> patchResourceSpecification(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceSpecificationUpdateVO resourceSpecification);

	@io.micronaut.http.annotation.Get("/resourceSpecification/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ResourceSpecificationVO>> retrieveResourceSpecification(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);
}