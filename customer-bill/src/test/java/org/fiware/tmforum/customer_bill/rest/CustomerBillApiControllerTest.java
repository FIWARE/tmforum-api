package org.fiware.tmforum.customer_bill.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.reactivex.Maybe;
import org.fiware.customer_bill.model.CustomerBillVO;
import org.fiware.tmforum.common.repository.ReferencesRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customer_bill.TMForumMapper;
import org.fiware.tmforum.customer_bill.TMForumMapperImpl;
import org.fiware.tmforum.customer_bill.domain.customer_bill.CustomerBill;
import org.fiware.tmforum.customer_bill.repository.CustomerBillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit Tests for CustomerBillApiController
 *
 * TODO:
 * - Tests for listCustomerBill
 * - Tests for patch
 */
public class CustomerBillApiControllerTest {

    private final TMForumMapper tmForumMapper = new TMForumMapperImpl();
    private final CustomerBillRepository customerBillRepository = mock(CustomerBillRepository.class);
    private final ReferencesRepository referencesRepository = mock(ReferencesRepository.class);
    private final ReferenceValidationService validationService = new ReferenceValidationService(referencesRepository);
    private CustomerBillApiController customerBillApiController;

    @BeforeEach
    public void setup() {
        customerBillApiController = new CustomerBillApiController(tmForumMapper, customerBillRepository, validationService);
    }

    @DisplayName("Customer bill received")
    @Test
    void testReceiveCustomerBill() {
        CustomerBillVO customerBillVO = new CustomerBillVO();
        customerBillVO.setBillNo("1234");
        customerBillVO.setId("id1");
        CustomerBill customerBill = tmForumMapper.map(customerBillVO);

        // Stub customer bill repo receive
        when(customerBillRepository.getCustomerBill("id1"))
                .thenReturn(Maybe.just(customerBill));

        // Retrieve customer
        HttpResponse<CustomerBillVO> customerBillVOHttpResponse =
                customerBillApiController.retrieveCustomerBill("id1", null).blockingGet();
        assertEquals(HttpStatus.OK,
                customerBillVOHttpResponse.getStatus(),
                "A customer bill response has OK status");
        assertTrue(customerBillVOHttpResponse.getBody().isPresent(), "A customer bill response has object body.");
        assertEquals(customerBillVO,
                customerBillVOHttpResponse.getBody().get(),
                "The same customer bill was returned");

    }
}
