package org.fiware.tmforum.customer_bill.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.fiware.customer_bill.model.CustomerBillOnDemandCreateVO;
import org.fiware.customer_bill.model.CustomerBillOnDemandVO;
import org.fiware.customer_bill.model.CustomerBillVO;
import org.fiware.customer_bill.model.RelatedPartyRefVO;
import org.fiware.tmforum.common.repository.ReferencesRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customer_bill.TMForumMapper;
import org.fiware.tmforum.customer_bill.TMForumMapperImpl;
import org.fiware.tmforum.customer_bill.domain.customer_bill.CustomerBill;
import org.fiware.tmforum.customer_bill.domain.customer_bill.CustomerBillOnDemand;
import org.fiware.tmforum.customer_bill.repository.CustomerBillOnDemandRepository;
import org.fiware.tmforum.customer_bill.repository.CustomerBillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit Tests for CustomerBillOnDemandApiController
 *
 * TODO:
 * - Tests for listCustomerBillOnDemand
 */
public class CustomerBillOnDemandApiControllerTest {

    private final TMForumMapper tmForumMapper = new TMForumMapperImpl();
    private final CustomerBillOnDemandRepository customerBillOnDemandRepository =
            mock(CustomerBillOnDemandRepository.class);
    private final ReferencesRepository referencesRepository = mock(ReferencesRepository.class);
    private final ReferenceValidationService validationService =
            new ReferenceValidationService(referencesRepository);
    private CustomerBillOnDemandApiController customerBillOnDemandApiController;

    @BeforeEach
    public void setup() {
        customerBillOnDemandApiController =
                new CustomerBillOnDemandApiController(tmForumMapper,
                        customerBillOnDemandRepository, validationService);
    }

    private CustomerBillOnDemandVO getCustomerBill(CustomerBillOnDemandVO customerBill) {
        return customerBill;
    }

    @DisplayName("CustomerBillOnDemand created")
    @Test
    void testCreateCustomerBillOnDemand() {
        CustomerBillOnDemandCreateVO customerCreateVO = new CustomerBillOnDemandCreateVO();
        customerCreateVO.setName("Customer bill on demand Name");
        CustomerBillOnDemandVO customerMockVO = tmForumMapper.map(customerCreateVO);

        RelatedPartyRefVO relatedPartyVO = new RelatedPartyRefVO();
        relatedPartyVO.setName("Related party ref name");
        relatedPartyVO.setRole("Related party ref role");
        relatedPartyVO.setId("id1");
        customerCreateVO.setRelatedParty(relatedPartyVO);

        // Stub customer repo create
        when(customerBillOnDemandRepository.createCustomerBillOnDemand(anyObject()))
                .thenReturn(Completable.fromAction(() -> getCustomerBill(customerMockVO)));

        // Stub references repo ref exists
        when(referencesRepository.referenceExists(anyString(), anyList()))
                .thenReturn(Maybe.just(relatedPartyVO));

        Single<HttpResponse<CustomerBillOnDemandVO>> singleResponse =
                customerBillOnDemandApiController.createCustomerBillOnDemand(customerCreateVO);
        CustomerBillOnDemandVO customerResponseVO = singleResponse.blockingGet().body();

        assertEquals(HttpStatus.CREATED,
                singleResponse.blockingGet().getStatus(),
                "A customer bill on demand response with status CREATED is expected.");
        assertTrue(singleResponse.blockingGet().getBody().isPresent(),
                "A customer bill on demand response contains object");
        assertEquals(customerCreateVO.getName(),
                customerResponseVO.getName(),
                "Returned customer bill on demand should have equal name compared to the created one");
    }

    @DisplayName("CustomerBillOnDemand received")
    @Test
    void testReceiveCustomerBillOnDemand() {
        CustomerBillOnDemandVO customerBillVO = new CustomerBillOnDemandVO();
        customerBillVO.setName("Customer bill on demand name");
        customerBillVO.setId("id1");
        CustomerBillOnDemand customerBill = tmForumMapper.map(customerBillVO);

        // Stub customer bill repo receive
        when(customerBillOnDemandRepository.getCustomerBillOnDemand("id1"))
                .thenReturn(Maybe.just(customerBill));

        // Retrieve customer
        HttpResponse<CustomerBillOnDemandVO> customerBillVOHttpResponse =
                customerBillOnDemandApiController.retrieveCustomerBillOnDemand("id1", null).blockingGet();
        assertEquals(HttpStatus.OK,
                customerBillVOHttpResponse.getStatus(),
                "A customer bill on demand response has OK status");
        assertTrue(customerBillVOHttpResponse.getBody().isPresent(), "A customer bill response has object body.");
        assertEquals(customerBillVO,
                customerBillVOHttpResponse.getBody().get(),
                "The same customer bill on demand was returned");

    }
}
