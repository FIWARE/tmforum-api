package org.fiware.tmforum.resourcefunction;

import org.fiware.resourcefunction.model.CharacteristicVO;
import org.fiware.resourcefunction.model.FeatureVO;
import org.fiware.resourcefunction.model.HealCreateVO;
import org.fiware.resourcefunction.model.HealVO;
import org.fiware.resourcefunction.model.MigrateCreateVO;
import org.fiware.resourcefunction.model.MigrateVO;
import org.fiware.resourcefunction.model.MonitorVO;
import org.fiware.resourcefunction.model.ResourceFunctionCreateVO;
import org.fiware.resourcefunction.model.ResourceFunctionUpdateVO;
import org.fiware.resourcefunction.model.ResourceFunctionVO;
import org.fiware.resourcefunction.model.ResourceGraphRelationshipVO;
import org.fiware.resourcefunction.model.ResourceGraphVO;
import org.fiware.resourcefunction.model.ResourceRefOrValueVO;
import org.fiware.resourcefunction.model.ResourceRelationshipVO;
import org.fiware.resourcefunction.model.ScaleCreateVO;
import org.fiware.resourcefunction.model.ScaleVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.mapping.MappingException;
import org.fiware.tmforum.common.domain.Characteristic;
import org.fiware.tmforum.common.domain.Feature;
import org.fiware.tmforum.resourcefunction.domain.Heal;
import org.fiware.tmforum.resourcefunction.domain.Migrate;
import org.fiware.tmforum.resourcefunction.domain.Monitor;
import org.fiware.tmforum.resourcefunction.domain.Resource;
import org.fiware.tmforum.resourcefunction.domain.ResourceFunction;
import org.fiware.tmforum.resourcefunction.domain.ResourceGraph;
import org.fiware.tmforum.resourcefunction.domain.ResourceGraphRelationship;
import org.fiware.tmforum.resourcefunction.domain.ResourceRelationship;
import org.fiware.tmforum.resourcefunction.domain.Scale;
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

    // monitor

    MonitorVO map(Monitor monitor);

    // heal

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    HealVO map(HealCreateVO healCreateVO, URI id);

    HealVO map(Heal heal);

    Heal map(HealVO healVO);

    // migrate

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    MigrateVO map(MigrateCreateVO migrateCreateVO, URI id);

    MigrateVO map(Migrate migrate);

    Migrate map(MigrateVO migrateVO);

    // scale

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ScaleVO map(ScaleCreateVO scaleCreateVOVO, URI id);

    ScaleVO map(Scale scale);

    Scale map(ScaleVO scaleVO);

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

        ResourceGraphRelationship resourceGraphRelationship = new ResourceGraphRelationship(resourceGraphRelationshipVO.getResourceGraph().getId());

        resourceGraphRelationship.setRelationshipType(resourceGraphRelationshipVO.getRelationshipType());
        resourceGraphRelationship.setHref(URI.create(resourceGraphRelationshipVO.getResourceGraph().getHref()));
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


