package org.fiware.tmforum.resourceinventory;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourceinventory.model.EventSubscriptionVO;
import org.fiware.resourceinventory.model.ResourceCreateVO;
import org.fiware.resourceinventory.model.ResourceUpdateVO;
import org.fiware.resourceinventory.model.ResourceVO;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.Resource;
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
    ResourceVO map(ResourceCreateVO resourceCreateVO, URI id);

    ResourceVO map(Resource resource);

    Resource map(ResourceVO resourceVO);

    @Mapping(target = "id", source = "id")
    Resource map(ResourceUpdateVO resourceUpdateVO, String id);

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
}


