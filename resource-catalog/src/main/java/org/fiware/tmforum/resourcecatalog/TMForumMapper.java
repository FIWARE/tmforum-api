package org.fiware.tmforum.resourcecatalog;

import org.fiware.resourcecatalog.model.CharacteristicVO;
import org.fiware.resourcecatalog.model.FeatureVO;
import org.fiware.resourcecatalog.model.ResourceFunctionCreateVO;
import org.fiware.resourcecatalog.model.ResourceFunctionUpdateVO;
import org.fiware.resourcecatalog.model.ResourceFunctionVO;
import org.fiware.resourcecatalog.model.ResourceGraphRelationshipVO;
import org.fiware.resourcecatalog.model.ResourceGraphVO;
import org.fiware.resourcecatalog.model.ResourceRefOrValueVO;
import org.fiware.resourcecatalog.model.ResourceRelationshipVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.mapping.MappingException;
import org.fiware.tmforum.resourcecatalog.domain.Characteristic;
import org.fiware.tmforum.resourcecatalog.domain.Feature;
import org.fiware.tmforum.resourcecatalog.domain.Resource;
import org.fiware.tmforum.resourcecatalog.domain.ResourceFunction;
import org.fiware.tmforum.resourcecatalog.domain.ResourceGraph;
import org.fiware.tmforum.resourcecatalog.domain.ResourceGraphRelationship;
import org.fiware.tmforum.resourcecatalog.domain.ResourceRelationship;
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

    // resource function

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ResourceFunctionVO map(ResourceFunctionCreateVO resourceFunctionCreateVO, URI id);

    ResourceFunctionVO map(ResourceFunction resourceFunction);

    ResourceFunction map(ResourceFunctionVO resourceFunctionVO);

    @Mapping(target = "id", source = "id")
    ResourceFunction map(ResourceFunctionUpdateVO resourceFunctionUpdateVO, String id);

    // sub-entities

    @Mapping(target = "id", qualifiedByName = {"IdHelper", "FromNgsiLd"})
    FeatureVO map(Feature feature);

    @Mapping(target = "id", qualifiedByName = {"IdHelper", "FromNgsiLd"})
    CharacteristicVO map(Characteristic characteristic);

    @Mapping(target = "id", qualifiedByName = {"IdHelper", "FromNgsiLd"})
    ResourceGraphVO map(ResourceGraph resourceGraph);

    @Mapping(target = "id", qualifiedByName = {"IdHelper", "FromNgsiLd"})
    @Mapping(target = "href", source = "id", qualifiedByName = {"IdHelper", "FromNgsiLd"})
    ResourceRefOrValueVO map(Resource resource);

    Resource map(ResourceRefOrValueVO resourceRefOrValueVO);

    default ResourceGraphRelationship map(ResourceGraphRelationshipVO resourceGraphRelationshipVO) {
        if (resourceGraphRelationshipVO.getResourceGraph() == null || resourceGraphRelationshipVO.getResourceGraph().getId() == null) {
            throw new MappingException("No graph relationship without referencing a graph should exist.");
        }
        ResourceGraphRelationship resourceGraphRelationship = new ResourceGraphRelationship(resourceGraphRelationshipVO.getId());
        resourceGraphRelationship.setRelationshipType(resourceGraphRelationshipVO.getRelationshipType());
        resourceGraphRelationship.setHref(resourceGraphRelationshipVO.getHref());
        resourceGraphRelationship.setAtBaseType(resourceGraphRelationshipVO.getAtBaseType());
        resourceGraphRelationship.setAtType(resourceGraphRelationshipVO.getAtType());
        resourceGraphRelationship.setAtReferredType(resourceGraphRelationshipVO.getResourceGraph().getAtReferredType());
        resourceGraphRelationship.setName(resourceGraphRelationshipVO.getResourceGraph().getName());
        return resourceGraphRelationship;
    }

    default ResourceRelationship map(ResourceRelationshipVO resourceRelationshipVO) {
        if (resourceRelationshipVO.getResource() == null) {
            throw new MappingException("A resource relationship need to have a resource or a reference.");
        }
        if (resourceRelationshipVO.getResource().getId() == null) {
            throw new MappingException("A resource needs to have an id.");
        }

        Resource resource = map(resourceRelationshipVO.getResource());
        // relationship gets the same id to allow easier resolution
        ResourceRelationship resourceRelationship = new ResourceRelationship(resource.getId());
        resourceRelationship.setResource(resource);
        resourceRelationship.setRelationshipType(resourceRelationshipVO.getRelationshipType());
        resourceRelationship.setAtBaseType(resourceRelationshipVO.getAtBaseType());
        resourceRelationship.setAtType(resourceRelationshipVO.getAtType());
        resourceRelationship.setAtSchemaLocation(resourceRelationshipVO.getAtSchemaLocation());
        return resourceRelationship;
    }

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
}


