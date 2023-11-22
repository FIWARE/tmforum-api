package org.fiware.resourcecatalog.api;

import org.fiware.resourcecatalog.model.*;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
public interface ResourceCatalogApi {

	java.lang.String PATH_CREATE_RESOURCE_CATALOG = "/resourceCatalog";
	java.lang.String PATH_DELETE_RESOURCE_CATALOG = "/resourceCatalog/{id}";
	java.lang.String PATH_LIST_RESOURCE_CATALOG = "/resourceCatalog";
	java.lang.String PATH_PATCH_RESOURCE_CATALOG = "/resourceCatalog/{id}";
	java.lang.String PATH_RETRIEVE_RESOURCE_CATALOG = "/resourceCatalog/{id}";

	@io.micronaut.http.annotation.Post("/resourceCatalog")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ResourceCatalogVO>> createResourceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCatalogCreateVO resourceCatalog);

	@io.micronaut.http.annotation.Delete("/resourceCatalog/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<Object>> deleteResourceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Get("/resourceCatalog")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<java.util.List<ResourceCatalogVO>>> listResourceCatalog(
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
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ResourceCatalogVO>> patchResourceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCatalogUpdateVO resourceCatalog);

	@io.micronaut.http.annotation.Get("/resourceCatalog/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ResourceCatalogVO>> retrieveResourceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);
}