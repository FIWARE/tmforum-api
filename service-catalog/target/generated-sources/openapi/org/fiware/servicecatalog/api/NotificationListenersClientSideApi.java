package org.fiware.servicecatalog.api;

import org.fiware.servicecatalog.model.*;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
public interface NotificationListenersClientSideApi {

	java.lang.String PATH_LISTEN_TO_SERVICE_CANDIDATE_CHANGE_EVENT = "/listener/serviceCandidateChangeEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_CANDIDATE_CREATE_EVENT = "/listener/serviceCandidateCreateEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_CANDIDATE_DELETE_EVENT = "/listener/serviceCandidateDeleteEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_CATALOG_BATCH_EVENT = "/listener/serviceCatalogBatchEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_CATALOG_CHANGE_EVENT = "/listener/serviceCatalogChangeEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_CATALOG_CREATE_EVENT = "/listener/serviceCatalogCreateEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_CATALOG_DELETE_EVENT = "/listener/serviceCatalogDeleteEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_CATEGORY_CHANGE_EVENT = "/listener/serviceCategoryChangeEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_CATEGORY_CREATE_EVENT = "/listener/serviceCategoryCreateEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_CATEGORY_DELETE_EVENT = "/listener/serviceCategoryDeleteEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_SPECIFICATION_CHANGE_EVENT = "/listener/serviceSpecificationChangeEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_SPECIFICATION_CREATE_EVENT = "/listener/serviceSpecificationCreateEvent";
	java.lang.String PATH_LISTEN_TO_SERVICE_SPECIFICATION_DELETE_EVENT = "/listener/serviceSpecificationDeleteEvent";

	@io.micronaut.http.annotation.Post("/listener/serviceCandidateChangeEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceCandidateChangeEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCandidateChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCandidateCreateEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceCandidateCreateEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCandidateCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCandidateDeleteEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceCandidateDeleteEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCandidateDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCatalogBatchEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceCatalogBatchEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCatalogBatchEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCatalogChangeEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceCatalogChangeEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCatalogChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCatalogCreateEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceCatalogCreateEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCatalogCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCatalogDeleteEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceCatalogDeleteEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCatalogDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCategoryChangeEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceCategoryChangeEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCategoryChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCategoryCreateEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceCategoryCreateEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCategoryCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCategoryDeleteEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceCategoryDeleteEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceCategoryDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceSpecificationChangeEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceSpecificationChangeEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceSpecificationChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceSpecificationCreateEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceSpecificationCreateEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceSpecificationCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceSpecificationDeleteEvent")
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	reactor.core.publisher.Mono<io.micronaut.http.HttpResponse<EventSubscriptionVO>> listenToServiceSpecificationDeleteEvent(
			@io.micronaut.core.annotation.NonNull
			@io.micronaut.http.annotation.Body
			ServiceSpecificationDeleteEventVO data);
}