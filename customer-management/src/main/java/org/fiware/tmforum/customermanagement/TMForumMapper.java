package org.fiware.tmforum.customermanagement;

import io.github.wistefan.mapping.MappingException;
import org.fiware.customermanagement.model.CustomerCreateVO;
import org.fiware.customermanagement.model.CustomerUpdateVO;
import org.fiware.customermanagement.model.CustomerVO;
import org.fiware.customermanagement.model.EventSubscriptionVO;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.customermanagement.domain.Customer;
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

    // customer management

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    CustomerVO map(CustomerCreateVO customerCreateVO, URI id);

    CustomerVO map(Customer customer);

    Customer map(CustomerVO serviceCatalogVO);

    @Mapping(target = "id", source = "id")
    Customer map(CustomerUpdateVO customerUpdateVO, String id);

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
}


