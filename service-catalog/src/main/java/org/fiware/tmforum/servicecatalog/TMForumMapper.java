package org.fiware.tmforum.servicecatalog;

import io.github.wistefan.mapping.MappingException;
import org.fiware.servicecatalog.model.*;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.ResourceSpecificationRef;
import org.fiware.tmforum.service.ServiceCandidate;
import org.fiware.tmforum.service.ServiceCategory;
import org.fiware.tmforum.service.ServiceCategoryRef;
import org.fiware.tmforum.service.ServiceSpecificationRelationship;
import org.fiware.tmforum.servicecatalog.domain.ServiceCatalog;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public interface TMForumMapper {

    // service catalog

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ServiceCatalogVO map(ServiceCatalogCreateVO serviceCatalogCreateVO, URI id);

    ServiceCatalogVO map(ServiceCatalog serviceCatalog);

    ServiceCatalog map(ServiceCatalogVO serviceCatalogVO);

    @Mapping(target = "id", source = "id")
    ServiceCatalog map(ServiceCatalogUpdateVO serviceCatalogUpdateVO, String id);

    // service candidate

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ServiceCandidateVO map(ServiceCandidateCreateVO serviceCandidateCreateVO, URI id);

    ServiceCandidateVO map(ServiceCandidate serviceCandidate);

    ServiceCandidate map(ServiceCandidateVO serviceCandidateVO);

    @Mapping(target = "id", source = "id")
    ServiceCandidate map(ServiceCandidateUpdateVO serviceCandidateUpdateVO, String id);

    // service category

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ServiceCategoryVO map(ServiceCategoryCreateVO serviceCategoryCreateVO, URI id);

    ServiceCategoryVO map(ServiceCategory serviceCategory);

    ServiceCategory map(ServiceCategoryVO serviceCandidateVO);

    @Mapping(target = "id", source = "id")
    ServiceCategory map(ServiceCategoryUpdateVO serviceCategoryUpdateVO, String id);

    // service specification

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ServiceSpecificationVO map(ServiceSpecificationCreateVO serviceSpecificationCreateVO, URI id);

    ServiceSpecificationVO map(ServiceSpecification serviceSpecification);

    ServiceSpecification map(ServiceSpecificationVO serviceSpecificationVO);

    @Mapping(target = "id", source = "id")
    ServiceSpecification map(ServiceSpecificationUpdateVO serviceSpecificationUpdateVO, String id);

    @Mapping(target = "query", source = "rawQuery")
    EventSubscriptionVO map(Subscription subscription);

    default URL map(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new MappingException(String.format("%s is not a URL.", value), e);
        }
    }

    default String map(URL value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    default URI mapToURI(String value) {
        if (value == null) {
            return null;
        }
        return URI.create(value);
    }

    default String mapFromURI(URI value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    default ResourceSpecificationRef mapFromResourceSpecId(String id) {
        if (id == null) {
            return null;
        }
        return new ResourceSpecificationRef(id);
    }

    default ServiceCategoryRef mapFromServiceCategoryRefId(String id) {
        if (id == null) {
            return null;
        }
        return new ServiceCategoryRef(id);
    }

    default ServiceSpecificationRelationship mapFromServiceSpecificationRelationshipId(String id) {
        if (id == null) {
            return null;
        }
        return new ServiceSpecificationRelationship(URI.create(id));
    }

    default String mapFromServiceSpecificationRelationship(ServiceSpecificationRelationship serviceSpecificationRelationship) {
        if (serviceSpecificationRelationship == null) {
            return null;
        }
        return serviceSpecificationRelationship.getEntityId().toString();
    }

    default String mapFromServiceCategoryRef(ServiceCategoryRef serviceCategoryRef) {
        if (serviceCategoryRef == null) {
            return null;
        }
        return serviceCategoryRef.getEntityId().toString();
    }

    default String mapFromResourceSpecificationRef(ResourceSpecificationRef resourceSpecificationRef) {
        if (resourceSpecificationRef == null) {
            return null;
        }
        return resourceSpecificationRef.getEntityId().toString();
    }


}


