package org.fiware.tmforum.customermanagement;

import io.github.wistefan.mapping.MappingException;
import org.checkerframework.checker.units.qual.C;
import org.fiware.customermanagement.model.*;
import org.fiware.tmforum.common.domain.Characteristic;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.customermanagement.domain.Customer;
import org.mapstruct.MapMapping;
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

    // customer management

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract CustomerVO map(CustomerCreateVO customerCreateVO, URI id);

    public abstract CustomerVO map(Customer customer);

    public abstract Customer map(CustomerVO serviceCatalogVO);

    @Mapping(target = "tmfValue", source = "value")
    public abstract Characteristic map(CharacteristicVO characteristicVO);

    @Mapping(target = "value", source = "tmfValue")
    public abstract CharacteristicVO map(Characteristic characteristic);

    @Mapping(target = "id", source = "id")
    public abstract Customer map(CustomerUpdateVO customerUpdateVO, String id);

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


