package org.fiware.tmforum.resourcefunction;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourcefunction.model.*;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.Resource;
import org.fiware.tmforum.resourcefunction.domain.*;
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

    Resource map(ResourceRefOrValueVO resourceRefOrValueVO);

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
}


