package org.fiware.servicecatalog.api;

import org.fiware.servicecatalog.model.*;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
public interface ServiceCategoryApi {

	java.lang.String PATH_CREATE_SERVICE_CATEGORY = "/serviceCategory";
	java.lang.String PATH_DELETE_SERVICE_CATEGORY = "/serviceCategory/{id}";
	java.lang.String PATH_LIST_SERVICE_CATEGORY = "/serviceCategory";
	java.lang.String PATH_PATCH_SERVICE_CATEGORY = "/serviceCategory/{id}";
	java.lang.String PATH_RETRIEVE_SERVICE_CATEGORY = "/serviceCategory/{id}";

	@io.micronaut.http.annotation.Post("/serviceCategory")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceCategoryVO>> createServiceCategory(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCategoryCreateVO serviceCategory);

	@io.micronaut.http.annotation.Delete("/serviceCategory/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<Object>> deleteServiceCategory(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Get("/serviceCategory")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<java.util.List<ServiceCategoryVO>>> listServiceCategory(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "offset")
			java.lang.Integer offset,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "limit")
			java.lang.Integer limit);

	@io.micronaut.http.annotation.Patch("/serviceCategory/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceCategoryVO>> patchServiceCategory(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCategoryUpdateVO serviceCategory);

	@io.micronaut.http.annotation.Get("/serviceCategory/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceCategoryVO>> retrieveServiceCategory(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);
}