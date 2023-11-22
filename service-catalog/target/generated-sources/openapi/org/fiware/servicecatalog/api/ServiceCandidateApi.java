package org.fiware.servicecatalog.api;

import org.fiware.servicecatalog.model.*;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
public interface ServiceCandidateApi {

	java.lang.String PATH_CREATE_SERVICE_CANDIDATE = "/serviceCandidate";
	java.lang.String PATH_DELETE_SERVICE_CANDIDATE = "/serviceCandidate/{id}";
	java.lang.String PATH_LIST_SERVICE_CANDIDATE = "/serviceCandidate";
	java.lang.String PATH_PATCH_SERVICE_CANDIDATE = "/serviceCandidate/{id}";
	java.lang.String PATH_RETRIEVE_SERVICE_CANDIDATE = "/serviceCandidate/{id}";

	@io.micronaut.http.annotation.Post("/serviceCandidate")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceCandidateVO>> createServiceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCandidateCreateVO serviceCandidate);

	@io.micronaut.http.annotation.Delete("/serviceCandidate/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<Object>> deleteServiceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Get("/serviceCandidate")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<java.util.List<ServiceCandidateVO>>> listServiceCandidate(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "offset")
			java.lang.Integer offset,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "limit")
			java.lang.Integer limit);

	@io.micronaut.http.annotation.Patch("/serviceCandidate/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceCandidateVO>> patchServiceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCandidateUpdateVO serviceCandidate);

	@io.micronaut.http.annotation.Get("/serviceCandidate/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ServiceCandidateVO>> retrieveServiceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);
}