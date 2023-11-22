package org.fiware.servicecatalog.api;

import org.fiware.servicecatalog.model.*;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
public interface ServiceCatalogApi {

	java.lang.String PATH_CREATE_SERVICE_CATALOG = "/serviceCatalog";
	java.lang.String PATH_DELETE_SERVICE_CATALOG = "/serviceCatalog/{id}";
	java.lang.String PATH_LIST_SERVICE_CATALOG = "/serviceCatalog";
	java.lang.String PATH_PATCH_SERVICE_CATALOG = "/serviceCatalog/{id}";
	java.lang.String PATH_RETRIEVE_SERVICE_CATALOG = "/serviceCatalog/{id}";

	@io.micronaut.http.annotation.Post("/serviceCatalog")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceCatalogVO>> createServiceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCatalogCreateVO serviceCatalog);

	@io.micronaut.http.annotation.Delete("/serviceCatalog/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<Object>> deleteServiceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Get("/serviceCatalog")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<java.util.List<ServiceCatalogVO>>> listServiceCatalog(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "offset")
			java.lang.Integer offset,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "limit")
			java.lang.Integer limit);

	@io.micronaut.http.annotation.Patch("/serviceCatalog/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceCatalogVO>> patchServiceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCatalogUpdateVO serviceCatalog);

	@io.micronaut.http.annotation.Get("/serviceCatalog/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceCatalogVO>> retrieveServiceCatalog(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);
}