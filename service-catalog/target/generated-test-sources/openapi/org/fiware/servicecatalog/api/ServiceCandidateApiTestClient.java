package org.fiware.servicecatalog.api;

import org.fiware.servicecatalog.model.*;

/** Test client for {@link ServiceCandidateApi}. **/
@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.http.client.annotation.Client("/")
public interface ServiceCandidateApiTestClient {

	@io.micronaut.http.annotation.Post("/serviceCandidate")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ServiceCandidateVO> createServiceCandidate(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCandidateCreateVO serviceCandidate);

	@io.micronaut.http.annotation.Post("/serviceCandidate")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ServiceCandidateVO> createServiceCandidate(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCandidateCreateVO serviceCandidate);
	@io.micronaut.http.annotation.Delete("/serviceCandidate/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<?> deleteServiceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);

	@io.micronaut.http.annotation.Delete("/serviceCandidate/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<?> deleteServiceCandidate(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id);
	@io.micronaut.http.annotation.Get("/serviceCandidate")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<java.util.List<ServiceCandidateVO>> listServiceCandidate(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "offset")
			java.lang.Integer offset,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "limit")
			java.lang.Integer limit);

	@io.micronaut.http.annotation.Get("/serviceCandidate")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<java.util.List<ServiceCandidateVO>> listServiceCandidate(
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
	@io.micronaut.http.annotation.Patch("/serviceCandidate/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ServiceCandidateVO> patchServiceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCandidateUpdateVO serviceCandidate);

	@io.micronaut.http.annotation.Patch("/serviceCandidate/{id}")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ServiceCandidateVO> patchServiceCandidate(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCandidateUpdateVO serviceCandidate);
	@io.micronaut.http.annotation.Get("/serviceCandidate/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ServiceCandidateVO> retrieveServiceCandidate(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);

	@io.micronaut.http.annotation.Get("/serviceCandidate/{id}")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<ServiceCandidateVO> retrieveServiceCandidate(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.PathVariable(value = "id")
			java.lang.String id,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.QueryValue(value = "fields")
			java.lang.String fields);}
