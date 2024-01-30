package org.fiware.tmforum.resourcecatalog;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourcecatalog.model.*;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.*;
import org.fiware.tmforum.resourcecatalog.domain.ResourceCatalog;
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

    // resource catalog

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ResourceCatalogVO map(ResourceCatalogCreateVO resourceCatalogCreateVO, URI id);

    ResourceCatalogVO map(ResourceCatalog resourceCatalog);

    ResourceCatalog map(ResourceCatalogVO resourceCatalogVO);

    @Mapping(target = "id", source = "id")
    ResourceCatalog map(ResourceCatalogUpdateVO resourceCatalogUpdateVO, String id);

    // resource specification

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ResourceSpecificationVO map(ResourceSpecificationCreateVO resourceSpecificationCreateVO, URI id);

    ResourceSpecificationVO map(ResourceSpecification resourceSpecification);

    ResourceSpecification map(ResourceSpecificationVO resourceCandidateVO);

    @Mapping(target = "id", source = "id")
    ResourceSpecification map(ResourceSpecificationUpdateVO resourceSpecificationUpdateVO, String id);

    // resource catalog

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ResourceCandidateVO map(ResourceCandidateCreateVO resourceCandidateCreateVO, URI id);

    ResourceCandidateVO map(ResourceCandidate resourceCandidate);

    ResourceCandidate map(ResourceCandidateVO resourceCandidateVO);

    @Mapping(target = "id", source = "id")
    ResourceCandidate map(ResourceCandidateUpdateVO resourceCandidateUpdateVO, String id);

    // resource catalog

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ResourceCategoryVO map(ResourceCategoryCreateVO resourceCategoryCreateVO, URI id);

    ResourceCategoryVO map(ResourceCategory resourceCategory);

    ResourceCategory map(ResourceCategoryVO resourceCategoryVO);

    @Mapping(target = "id", source = "id")
    ResourceCategory map(ResourceCategoryUpdateVO resourceCategoryUpdateVO, String id);

    @Mapping(target = "query", source = "rawQuery")
    EventSubscriptionVO map(TMForumSubscription subscription);

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

    default ResourceCategoryRef mapFromCategoryId(String id) {
        if (id == null) {
            return null;
        }
        return new ResourceCategoryRef(URI.create(id));
    }

    default String mapFromCategoryRef(ResourceCategoryRef categoryRef) {
        if (categoryRef == null) {
            return null;
        }
        return categoryRef.getEntityId().toString();
    }

    default String mapFromResourceSpecificationRef(ResourceSpecificationRef resourceSpecificationRef) {
        if (resourceSpecificationRef == null) {
            return null;
        }
        return resourceSpecificationRef.getEntityId().toString();
    }
}


