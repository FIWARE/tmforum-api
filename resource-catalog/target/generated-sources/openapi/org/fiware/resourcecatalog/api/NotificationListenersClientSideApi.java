package org.fiware.resourcecatalog.api;

import org.fiware.resourcecatalog.model.*;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
public interface NotificationListenersClientSideApi {

	java.lang.String PATH_LISTEN_TO_EXPORT_JOB_CREATE_EVENT = "/listener/exportJobCreateEvent";
	java.lang.String PATH_LISTEN_TO_EXPORT_JOB_STATE_CHANGE_EVENT = "/listener/exportJobStateChangeEvent";
	java.lang.String PATH_LISTEN_TO_IMPORT_JOB_CREATE_EVENT = "/listener/importJobCreateEvent";
	java.lang.String PATH_LISTEN_TO_IMPORT_JOB_STATE_CHANGE_EVENT = "/listener/importJobStateChangeEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_CANDIDATE_CHANGE_EVENT = "/listener/resourceCandidateChangeEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_CANDIDATE_CREATE_EVENT = "/listener/resourceCandidateCreateEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_CANDIDATE_DELETE_EVENT = "/listener/resourceCandidateDeleteEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_CATALOG_CHANGE_EVENT = "/listener/resourceCatalogChangeEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_CATALOG_CREATE_EVENT = "/listener/resourceCatalogCreateEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_CATALOG_DELETE_EVENT = "/listener/resourceCatalogDeleteEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_CATEGORY_CHANGE_EVENT = "/listener/resourceCategoryChangeEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_CATEGORY_CREATE_EVENT = "/listener/resourceCategoryCreateEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_CATEGORY_DELETE_EVENT = "/listener/resourceCategoryDeleteEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_SPECIFICATION_CHANGE_EVENT = "/listener/resourceSpecificationChangeEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_SPECIFICATION_CREATE_EVENT = "/listener/resourceSpecificationCreateEvent";
	java.lang.String PATH_LISTEN_TO_RESOURCE_SPECIFICATION_DELETE_EVENT = "/listener/resourceSpecificationDeleteEvent";

	@io.micronaut.http.annotation.Post("/listener/exportJobCreateEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToExportJobCreateEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ExportJobCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/exportJobStateChangeEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToExportJobStateChangeEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ExportJobStateChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/importJobCreateEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToImportJobCreateEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ImportJobCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/importJobStateChangeEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToImportJobStateChangeEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ImportJobStateChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCandidateChangeEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceCandidateChangeEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCandidateChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCandidateCreateEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceCandidateCreateEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCandidateCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCandidateDeleteEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceCandidateDeleteEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCandidateDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCatalogChangeEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceCatalogChangeEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCatalogChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCatalogCreateEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceCatalogCreateEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCatalogCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCatalogDeleteEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceCatalogDeleteEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCatalogDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCategoryChangeEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceCategoryChangeEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCategoryChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCategoryCreateEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceCategoryCreateEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCategoryCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceCategoryDeleteEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceCategoryDeleteEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceCategoryDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceSpecificationChangeEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceSpecificationChangeEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceSpecificationChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceSpecificationCreateEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceSpecificationCreateEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceSpecificationCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/resourceSpecificationDeleteEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToResourceSpecificationDeleteEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ResourceSpecificationDeleteEventVO data);
}