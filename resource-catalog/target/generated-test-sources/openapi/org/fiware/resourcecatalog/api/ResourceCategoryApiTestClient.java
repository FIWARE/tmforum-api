package org.fiware.resourcecatalog.api;

import org.fiware.resourcecatalog.model.*;

/** Test client for {@link ResourceCategoryApi}. **/
@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.http.client.annotation.Client("/")
public interface ResourceCategoryApiTestClient {

	@io.micronaut.http.annotation.Post("/resourceCategory")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCategoryVO> createResourceCategory(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCategoryCreateVO resourceCategory);

	@io.micronaut.http.annotation.Post("/resourceCategory")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCategoryVO> createResourceCategory(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCategoryCreateVO resourceCategory);
	@io.micronaut.http.annotation.Delete("/resourceCategory/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<?> deleteResourceCategory(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Delete("/resourceCategory/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<?> deleteResourceCategory(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);
	@io.micronaut.http.annotation.Get("/resourceCategory")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<java.util.List<ResourceCategoryVO>> listResourceCategory(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "offset")
			java.lang.Integer offset,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "limit")
			java.lang.Integer limit);

	@io.micronaut.http.annotation.Get("/resourceCategory")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<java.util.List<ResourceCategoryVO>> listResourceCategory(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "offset")
			java.lang.Integer offset,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "limit")
			java.lang.Integer limit);
	@io.micronaut.http.annotation.Patch("/resourceCategory/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCategoryVO> patchResourceCategory(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCategoryUpdateVO resourceCategory);

	@io.micronaut.http.annotation.Patch("/resourceCategory/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCategoryVO> patchResourceCategory(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCategoryUpdateVO resourceCategory);
	@io.micronaut.http.annotation.Get("/resourceCategory/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCategoryVO> retrieveResourceCategory(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);

	@io.micronaut.http.annotation.Get("/resourceCategory/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCategoryVO> retrieveResourceCategory(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);}
