package org.fiware.resourcecatalog.api;

import org.fiware.resourcecatalog.model.*;

/** Test client for {@link ResourceCatalogApi}. **/
@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.http.client.annotation.Client("/")
public interface ResourceCatalogApiTestClient {

	@io.micronaut.http.annotation.Post("/resourceCatalog")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCatalogVO> createResourceCatalog(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCatalogCreateVO resourceCatalog);

	@io.micronaut.http.annotation.Post("/resourceCatalog")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCatalogVO> createResourceCatalog(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCatalogCreateVO resourceCatalog);
	@io.micronaut.http.annotation.Delete("/resourceCatalog/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<?> deleteResourceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Delete("/resourceCatalog/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<?> deleteResourceCatalog(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);
	@io.micronaut.http.annotation.Get("/resourceCatalog")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<java.util.List<ResourceCatalogVO>> listResourceCatalog(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "offset")
			java.lang.Integer offset,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "limit")
			java.lang.Integer limit);

	@io.micronaut.http.annotation.Get("/resourceCatalog")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<java.util.List<ResourceCatalogVO>> listResourceCatalog(
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
	@io.micronaut.http.annotation.Patch("/resourceCatalog/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCatalogVO> patchResourceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCatalogUpdateVO resourceCatalog);

	@io.micronaut.http.annotation.Patch("/resourceCatalog/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCatalogVO> patchResourceCatalog(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCatalogUpdateVO resourceCatalog);
	@io.micronaut.http.annotation.Get("/resourceCatalog/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCatalogVO> retrieveResourceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);

	@io.micronaut.http.annotation.Get("/resourceCatalog/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ResourceCatalogVO> retrieveResourceCatalog(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);}
