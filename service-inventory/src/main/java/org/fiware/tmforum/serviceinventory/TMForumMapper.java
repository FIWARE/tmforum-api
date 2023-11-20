package org.fiware.tmforum.serviceinventory;

import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.resource.*;
import io.github.wistefan.mapping.MappingException;
import org.fiware.serviceinventory.model.*;
import org.fiware.tmforum.serviceinventory.domain.*;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.ResourceSpecificationRef;
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

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ServiceVO map(ServiceCreateVO billFormatCreateVO, URI id);

    ServiceVO map(Service billFormat);

    @Mapping(target = "href", source = "id")
    Service map(ServiceVO billFormatVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ServiceVO map(ServiceUpdateVO billFormatUpdateVO, String id);

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

    default ResourceRef mapResourceRefId(String id) {
        if (id == null) {
            return null;
        }
        return new ResourceRef(id);
    }

}
