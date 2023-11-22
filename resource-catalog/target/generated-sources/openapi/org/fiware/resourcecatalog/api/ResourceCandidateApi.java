package org.fiware.resourcecatalog.api;

import org.fiware.resourcecatalog.model.*;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
public interface ResourceCandidateApi {

	java.lang.String PATH_CREATE_RESOURCE_CANDIDATE = "/resourceCandidate";
	java.lang.String PATH_DELETE_RESOURCE_CANDIDATE = "/resourceCandidate/{id}";
	java.lang.String PATH_LIST_RESOURCE_CANDIDATE = "/resourceCandidate";
	java.lang.String PATH_PATCH_RESOURCE_CANDIDATE = "/resourceCandidate/{id}";
	java.lang.String PATH_RETRIEVE_RESOURCE_CANDIDATE = "/resourceCandidate/{id}";

	@io.micronaut.http.annotation.Post("/resourceCandidate")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ResourceCandidateVO>> createResourceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCandidateCreateVO resourceCandidate);

	@io.micronaut.http.annotation.Delete("/resourceCandidate/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<Object>> deleteResourceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Get("/resourceCandidate")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<java.util.List<ResourceCandidateVO>>> listResourceCandidate(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "offset")
			java.lang.Integer offset,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "limit")
			java.lang.Integer limit);

	@io.micronaut.http.annotation.Patch("/resourceCandidate/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ResourceCandidateVO>> patchResourceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCandidateUpdateVO resourceCandidate);

	@io.micronaut.http.annotation.Get("/resourceCandidate/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<ResourceCandidateVO>> retrieveResourceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);
}