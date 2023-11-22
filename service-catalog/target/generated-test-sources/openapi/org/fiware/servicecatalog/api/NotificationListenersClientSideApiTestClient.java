package org.fiware.servicecatalog.api;

import org.fiware.servicecatalog.model.*;

/** Test client for {@link NotificationListenersClientSideApi}. **/
@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.http.client.annotation.Client("/")
public interface NotificationListenersClientSideApiTestClient {

	@io.micronaut.http.annotation.Post("/listener/serviceCandidateChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCandidateChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCandidateChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCandidateChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCandidateChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCandidateChangeEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceCandidateCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCandidateCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCandidateCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCandidateCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCandidateCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCandidateCreateEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceCandidateDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCandidateDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCandidateDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCandidateDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCandidateDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCandidateDeleteEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceCatalogBatchEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCatalogBatchEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCatalogBatchEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCatalogBatchEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCatalogBatchEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCatalogBatchEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceCatalogChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCatalogChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCatalogChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCatalogChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCatalogChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCatalogChangeEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceCatalogCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCatalogCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCatalogCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCatalogCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCatalogCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCatalogCreateEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceCatalogDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCatalogDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCatalogDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCatalogDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCatalogDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCatalogDeleteEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceCategoryChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCategoryChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCategoryChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCategoryChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCategoryChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCategoryChangeEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceCategoryCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCategoryCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCategoryCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCategoryCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCategoryCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCategoryCreateEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceCategoryDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCategoryDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCategoryDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceCategoryDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceCategoryDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceCategoryDeleteEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceSpecificationChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceSpecificationChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceSpecificationChangeEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceSpecificationChangeEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceSpecificationChangeEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceSpecificationChangeEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceSpecificationCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceSpecificationCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceSpecificationCreateEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceSpecificationCreateEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceSpecificationCreateEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceSpecificationCreateEventVO data);
	@io.micronaut.http.annotation.Post("/listener/serviceSpecificationDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceSpecificationDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceSpecificationDeleteEventVO data);

	@io.micronaut.http.annotation.Post("/listener/serviceSpecificationDeleteEvent")
	@io.micronaut.http.annotation.Produces({ "application/json;charset=utf-8" })
	@io.micronaut.http.annotation.Consumes({ "application/json;charset=utf-8" })
	io.micronaut.http.HttpResponse<EventSubscriptionVO> listenToServiceSpecificationDeleteEvent(
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Header(io.micronaut.http.HttpHeaders.AUTHORIZATION)
			java.lang.String authorization,
			@io.micronaut.core.annotation.Nullable
			@io.micronaut.http.annotation.Body
			ServiceSpecificationDeleteEventVO data);}
