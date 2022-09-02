package org.fiware.tmforum.customer;

import org.fiware.customer.model.*;
import org.fiware.tmforum.customer.domain.ContactMedium;
import org.fiware.tmforum.customer.domain.MediumCharacteristic;
import org.fiware.tmforum.customer.domain.TimePeriod;
import org.fiware.tmforum.customer.domain.customer.Customer;
import org.fiware.tmforum.mapping.MappingException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330")
public interface TMForumMapper {

    String ID_TEMPLATE = "urn:ngsi-ld:%s:%s";


    // using inline expression, since else it might overwrite the String-String mapping
    @Mapping(target = "id", expression = "java(java.lang.String.format(ID_TEMPLATE, \"customer\", java.util.UUID.randomUUID()))")
    @Mapping(target = "href", ignore = true)
    CustomerVO map(CustomerCreateVO customerCreateVO);

    CustomerVO map(Customer customer);

    Customer map(CustomerVO customerVO);

    @Mapping(source = "characteristic", target = "mediumCharacteristic")
    ContactMedium map(ContactMediumVO contactMediumVO);

    @Mapping(target = "characteristic", source = "mediumCharacteristic")
    @Mapping(target = "validFor", source = "validFor")
    ContactMediumVO map(ContactMedium contactMedium);

    MediumCharacteristic map(MediumCharacteristicVO mediumCharacteristicVO);

    MediumCharacteristicVO map(MediumCharacteristic mediumCharacteristic);

    TimePeriodVO map(TimePeriod timePeriod);

    TimePeriod map(TimePeriodVO value);

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
