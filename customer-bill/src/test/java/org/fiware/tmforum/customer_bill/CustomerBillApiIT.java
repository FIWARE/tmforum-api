package org.fiware.tmforum.customer_bill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.customer_bill.model.CustomerBillOnDemandCreateVO;
import org.fiware.customer_bill.model.CustomerBillOnDemandVO;
import org.fiware.customer_bill.model.StateValuesVO;
import org.fiware.tmforum.customer_bill.rest.AppliedCustomerBillingRateApiController;
import org.fiware.tmforum.customer_bill.rest.CustomerBillApiController;
import org.fiware.tmforum.customer_bill.rest.CustomerBillOnDemandApiController;
import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        CustomerBillOnDemandCreateVO myFancyCustomerBillOnDemandCreate = getCustomerBillOnDemand();
        HttpResponse<CustomerBillOnDemandVO> myFancyCustomerBillOnDemandCreateResponse =
                customerBillOnDemandApiController.createCustomerBillOnDemand(myFancyCustomerBillOnDemandCreate)
                        .blockingGet();
        assertEquals(HttpStatus.CREATED,
                myFancyCustomerBillOnDemandCreateResponse.getStatus(),
                "CustomerBillOnDemand should have been created.");
        CustomerBillOnDemandVO myFancyCustomerBillOnDemand = myFancyCustomerBillOnDemandCreateResponse.body();

        // Test CustomerBillOnDemandApi retrieve
        HttpResponse<CustomerBillOnDemandVO> customerBillOnDemandResponse =
                customerBillOnDemandApiController.retrieveCustomerBillOnDemand(myFancyCustomerBillOnDemand.getId(), null)
                        .blockingGet();
        assertEquals(HttpStatus.OK,
                customerBillOnDemandResponse.getStatus(),
                "A CustomerBillOnDemand response is expected.");
        assertTrue(customerBillOnDemandResponse.getBody().isPresent(),
                "A CustomerBillOnDemand response is expected.");
        assertEquals(myFancyCustomerBillOnDemand,
                customerBillOnDemandResponse.getBody().get(),
                "The full CustomerBillOnDemand should be retrieved");

        // Test CustomerBillApi --> no create?

        // Test AppliedCustomerBillingRateApi --> no create?
    }

    private CustomerBillOnDemandCreateVO getCustomerBillOnDemand() throws JsonProcessingException {
        CustomerBillOnDemandCreateVO customerBillOnDemand = new CustomerBillOnDemandCreateVO();

        customerBillOnDemand.setDescription("My fancy description of CustomerBillOnDemand");
        customerBillOnDemand.setName("My fancy name");
        customerBillOnDemand.setLastUpdate("2022-03-04-08:00:00");
        customerBillOnDemand.setState(StateValuesVO.DONE);

        return customerBillOnDemand;
    }
}
