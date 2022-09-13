package org.fiware.tmforum.customer_bill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.customer_bill.model.*;
import org.fiware.tmforum.customer_bill.rest.AppliedCustomerBillingRateApiController;
import org.fiware.tmforum.customer_bill.rest.CustomerBillApiController;
import org.fiware.tmforum.customer_bill.rest.CustomerBillOnDemandApiController;
import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Customer Bill Management API
 *
 * TODO:
 * - Implement EventsSubscriptionApi + Tests
 * - Implement TimeTypeConverterRegistrar + Tests
 * - check how to create entities via CustomerBillApi and AppliedCustomerBillingRateApi
 */
@RequiredArgsConstructor
@MicronautTest(packages = {"org.fiware.tmforum.customer_bill"})
public class CustomerBillApiIT {

    private final ObjectMapper objectMapper;
    private final CustomerBillApiController customerBillApiController;
    private final CustomerBillOnDemandApiController customerBillOnDemandApiController;
    private final AppliedCustomerBillingRateApiController appliedCustomerBillingRateApiController;

    @Test
    void test() throws JsonProcessingException, ParseException {
        // Test CustomerBillOnDemandApi create
        CustomerBillOnDemandCreateVO myFancyCustomerBillOnDemandCreate =
                getCustomerBillOnDemand("My fancy first CustomerBillOnDemand", null);
        HttpResponse<CustomerBillOnDemandVO> myFancyCustomerBillOnDemandCreateResponse =
                customerBillOnDemandApiController.createCustomerBillOnDemand(myFancyCustomerBillOnDemandCreate)
                        .blockingGet();
        assertEquals(HttpStatus.CREATED,
                myFancyCustomerBillOnDemandCreateResponse.getStatus(),
                "CustomerBillOnDemand should have been created.");
        CustomerBillOnDemandVO myFancyCustomerBillOnDemand = myFancyCustomerBillOnDemandCreateResponse.body();

        // Test CustomerBillOnDemand 2nd create with related party
        RelatedPartyRefVO relatedPartyRefVO = new RelatedPartyRefVO();
        relatedPartyRefVO.setName("My related party ref");
        relatedPartyRefVO.setId(myFancyCustomerBillOnDemand.getId());
        relatedPartyRefVO.setRole("My related party ref role");
        CustomerBillOnDemandCreateVO myFancySecondCustomerBillOnDemandCreate =
                getCustomerBillOnDemand("My fancy 2nd CustomerBillOnDemand", relatedPartyRefVO);
        HttpResponse<CustomerBillOnDemandVO> myFancySecondCustomerBillOnDemandCreateResponse =
                customerBillOnDemandApiController.createCustomerBillOnDemand(myFancySecondCustomerBillOnDemandCreate)
                        .blockingGet();
        assertEquals(HttpStatus.CREATED,
                myFancySecondCustomerBillOnDemandCreateResponse.getStatus(),
                "2nd CustomerBillOnDemand should have been created.");
        CustomerBillOnDemandVO myFancySecondCustomerBillOnDemand = myFancySecondCustomerBillOnDemandCreateResponse.body();

        // Test CustomerBillOnDemandApi retrieve of 2nd CustomerBillOnDemand
        HttpResponse<CustomerBillOnDemandVO> customerBillOnDemandResponse =
                customerBillOnDemandApiController.retrieveCustomerBillOnDemand(
                        myFancySecondCustomerBillOnDemand.getId(), null)
                        .blockingGet();
        assertEquals(HttpStatus.OK,
                customerBillOnDemandResponse.getStatus(),
                "A CustomerBillOnDemand response is expected with status OK.");
        assertTrue(customerBillOnDemandResponse.getBody().isPresent(),
                "A CustomerBillOnDemand response is expected with body present.");
        assertEquals(myFancySecondCustomerBillOnDemand,
                customerBillOnDemandResponse.getBody().get(),
                "The full 2nd CustomerBillOnDemand should be retrieved");

        // TODO: Test CustomerBillApi --> no create?

        // TODO: Test AppliedCustomerBillingRateApi --> no create?
    }

    private CustomerBillOnDemandCreateVO getCustomerBillOnDemand(String name, RelatedPartyRefVO relatedPartyRefVO) throws JsonProcessingException {
        CustomerBillOnDemandCreateVO customerBillOnDemand = new CustomerBillOnDemandCreateVO();

        customerBillOnDemand.setDescription("My fancy description of CustomerBillOnDemand");
        customerBillOnDemand.setName(name);
        customerBillOnDemand.setLastUpdate("2022-03-04-08:00:00");

        BillingAccountRefVO billingAccountRefVO = new BillingAccountRefVO();
        billingAccountRefVO.setName("Fancy billing account ref");
        customerBillOnDemand.setBillingAccount(billingAccountRefVO);

        BillRefVO billRefVO = new BillRefVO();
        customerBillOnDemand.setCustomerBill(billRefVO);

        customerBillOnDemand.setState(StateValuesVO.DONE);

        if (relatedPartyRefVO != null) {
            customerBillOnDemand.setRelatedParty(relatedPartyRefVO);
        }

        return customerBillOnDemand;
    }
}
