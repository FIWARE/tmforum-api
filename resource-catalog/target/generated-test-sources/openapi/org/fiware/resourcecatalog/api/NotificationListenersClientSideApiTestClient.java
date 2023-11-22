package org.fiware.resourcecatalog.api;

import org.fiware.resourcecatalog.model.*;

/** Test client for {@link NotificationListenersClientSideApi}. **/
@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.http.client.annotation.Client("/")
public interface NotificationListenersClientSideApiTestClient {

	@io.micronaut.http.annotation.Post("/listener/exportJobCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToExportJobCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ExportJobCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/exportJobCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToExportJobCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ExportJobCreateEventVO data);
	@io.micronaut.http.annotation.Post("/listener/exportJobStateChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToExportJobStateChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ExportJobStateChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/exportJobStateChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToExportJobStateChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ExportJobStateChangeEventVO data);
	@io.micronaut.http.annotation.Post("/listener/importJobCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToImportJobCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ImportJobCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/importJobCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToImportJobCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ImportJobCreateEventVO data);
	@io.micronaut.http.annotation.Post("/listener/importJobStateChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToImportJobStateChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ImportJobStateChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/importJobStateChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToImportJobStateChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ImportJobStateChangeEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceCandidateChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCandidateChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCandidateChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCandidateChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCandidateChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCandidateChangeEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceCandidateCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCandidateCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCandidateCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCandidateCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCandidateCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCandidateCreateEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceCandidateDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCandidateDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCandidateDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCandidateDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCandidateDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCandidateDeleteEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceCatalogChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCatalogChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCatalogChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCatalogChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCatalogChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCatalogChangeEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceCatalogCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCatalogCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCatalogCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCatalogCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCatalogCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCatalogCreateEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceCatalogDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCatalogDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCatalogDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCatalogDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCatalogDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCatalogDeleteEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceCategoryChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCategoryChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCategoryChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCategoryChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCategoryChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCategoryChangeEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceCategoryCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCategoryCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCategoryCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCategoryCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCategoryCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCategoryCreateEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceCategoryDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCategoryDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCategoryDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCategoryDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceCategoryDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceCategoryDeleteEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceSpecificationChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceSpecificationChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceSpecificationChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceSpecificationChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceSpecificationChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceSpecificationChangeEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceSpecificationCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceSpecificationCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceSpecificationCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceSpecificationCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceSpecificationCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceSpecificationCreateEventVO data);
	@io.micronaut.http.annotation.Post("/listener/resourceSpecificationDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceSpecificationDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceSpecificationDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceSpecificationDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToResourceSpecificationDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ResourceSpecificationDeleteEventVO data);}
