package org.fiware.tmforum.serviceinventory;

import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
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

    // product

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    ServiceVO map(ServiceCreateVO productCreateVO, URI id);

    ServiceVO map(Service product);

    Service map(ServiceVO productVO);

    @Mapping(target = "id", source = "id")
    Service map(ServiceUpdateVO productUpdateVO, String id);

    //RelatedServiceOrderItemRef map (RelatedServiceOrderItemVO relatedServiceOrderItemVO);

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

    @Mapping(target = "query", source = "rawQuery")
    EventSubscriptionVO map(TMForumSubscription subscription);

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
