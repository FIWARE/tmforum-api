package org.fiware.tmforum.customer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.customer.model.*;
import org.fiware.tmforum.customer.rest.CustomerApiController;
import org.fiware.tmforum.customer.rest.CustomerApiControllerTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Customer Management API
 *
 * TODO:
 * - Implement EventsSubscriptionApi + Tests
 * - Implement NotificationListenersClientSideApi + Tests
 * - Implement TimeTypeConverterRegistrar + Tests
 */
@RequiredArgsConstructor
@MicronautTest(packages = {"org.fiware.tmforum.customer"})
class CustomerApiIT implements CustomerApiTestSpec {

    private final CustomerApiTestClient customerApiTestClient;

    private CustomerCreateVO customerCreateVO;
    private CustomerUpdateVO customerUpdateVO;
    private CustomerVO expectedCustomer;
    private String message;

    @ParameterizedTest
    @MethodSource("provideValidCustomers")
    public void createCustomer201(String message, CustomerCreateVO customerCreateVO, CustomerVO expectedCustomer) throws Exception {
        this.customerCreateVO = customerCreateVO;
        this.expectedCustomer = expectedCustomer;
        this.message = message;
        createCustomer201();
    }

    @Override
    public void createCustomer201() throws Exception {

        HttpResponse<CustomerVO> customerCreateResponse =
                callAndCatch(() -> customerApiTestClient.createCustomer(customerCreateVO));
        assertEquals(HttpStatus.CREATED, customerCreateResponse.getStatus(), message);

        CustomerVO createdCustomerVO = customerCreateResponse.body();
        expectedCustomer.setId(createdCustomerVO.getId());
        expectedCustomer.setHref(createdCustomerVO.getId());
        assertEquals(expectedCustomer, createdCustomerVO, message);

    }

    private static Stream<Arguments> provideValidCustomers() {
        List<Arguments> validCustomers = new ArrayList<>();

        CustomerCreateVO customerCreateVO = CustomerCreateVOTestExample.build();
        customerCreateVO.setEngagedParty(null);
        customerCreateVO.setRelatedParty(null);
        CustomerVO expectedCustomer = CustomerVOTestExample.build();
        expectedCustomer.setEngagedParty(null);
        expectedCustomer.setRelatedParty(null);
        validCustomers.add(
                Arguments.of("Empty customer should have been created.",
                        customerCreateVO, expectedCustomer));



        return validCustomers.stream();
    }

    // Helper method to catch potential http exceptions and return the status code.
    public <T> HttpResponse<T> callAndCatch(Callable<HttpResponse<T>> request) throws Exception {
        try {
            return request.call();
        } catch (HttpClientResponseException e) {
            return (HttpResponse<T>) e.getResponse();
        }
    }

}