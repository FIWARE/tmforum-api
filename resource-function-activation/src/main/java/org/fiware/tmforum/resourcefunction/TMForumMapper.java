package org.fiware.tmforum.resourcefunction;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourcefunction.model.*;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
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
public abstract class TMForumMapper extends BaseMapper {

    // resource function

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract ResourceFunctionVO map(ResourceFunctionCreateVO resourceFunctionCreateVO, URI id);

    public abstract ResourceFunctionVO map(ResourceFunction resourceFunction);

    public abstract ResourceFunction map(ResourceFunctionVO resourceFunctionVO);

    @Mapping(target = "id", source = "id")
    public abstract ResourceFunction map(ResourceFunctionUpdateVO resourceFunctionUpdateVO, String id);

    // monitor

    public abstract MonitorVO map(Monitor monitor);

    // heal

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract HealVO map(HealCreateVO healCreateVO, URI id);

    public abstract HealVO map(Heal heal);

    public abstract Heal map(HealVO healVO);

    // migrate

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract MigrateVO map(MigrateCreateVO migrateCreateVO, URI id);

    public abstract MigrateVO map(Migrate migrate);

    public abstract Migrate map(MigrateVO migrateVO);

    // scale

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract ScaleVO map(ScaleCreateVO scaleCreateVOVO, URI id);

    public abstract ScaleVO map(Scale scale);

    public abstract Scale map(ScaleVO scaleVO);

    // sub-entities

    public abstract Resource map(ResourceRefOrValueVO resourceRefOrValueVO);

    @Mapping(target = "query", source = "rawQuery")
    public abstract EventSubscriptionVO map(TMForumSubscription subscription);

    public URL map(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new MappingException(String.format("%s is not a URL.", value), e);
        }
    }

    public String map(URL value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public URI mapToURI(String value) {
        if (value == null) {
            return null;
        }
        return URI.create(value);
    }

    public String mapFromURI(URI value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}


