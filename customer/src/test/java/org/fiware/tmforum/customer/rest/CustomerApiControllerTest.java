package org.fiware.tmforum.customer.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.fiware.customer.model.*;
import org.fiware.tmforum.common.repository.ReferencesRepository;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customer.TMForumMapper;
import org.fiware.tmforum.customer.TMForumMapperImpl;
import org.fiware.tmforum.customer.domain.*;
import org.fiware.tmforum.customer.domain.customer.Customer;
import org.fiware.tmforum.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit Tests for CustomerApiController
 *
 * TODO:
 * * Tests for listCustomer
 * * Tests for delete
 */
public class CustomerApiControllerTest {

    private final TMForumMapper tmForumMapper = new TMForumMapperImpl();
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final ReferencesRepository referencesRepository = mock(ReferencesRepository.class);
    private final ReferenceValidationService validationService = new ReferenceValidationService(referencesRepository);
    private CustomerApiController customerApiController;

    @BeforeEach
    public void setup() {
        customerApiController = new CustomerApiController(tmForumMapper, customerRepository, validationService);
    }

    private CustomerVO getCustomer(CustomerVO customer) {
        return customer;
    }

    @DisplayName("Customer created")
    @Test
    void testCreateCustomer() {
        CustomerCreateVO customerCreateVO = new CustomerCreateVO();
        customerCreateVO.setName("Customer Name");
        CustomerVO customerMockVO = tmForumMapper.map(customerCreateVO);

        RelatedPartyVO relatedPartyVO = new RelatedPartyVO();
        relatedPartyVO.setName("Related party name");
        relatedPartyVO.setRole("Related party role");
        relatedPartyVO.setId("id1");
        customerCreateVO.setRelatedParty(List.of(relatedPartyVO));
        customerCreateVO.setEngagedParty(relatedPartyVO);

        // Stub customer repo create
        when(customerRepository.createCustomer(anyObject()))
                .thenReturn(Completable.fromAction(() -> getCustomer(customerMockVO)));

        // Stub references repo ref exists
        when(referencesRepository.referenceExists(anyString(), anyList()))
                .thenReturn(Maybe.just(relatedPartyVO));

        Single<HttpResponse<CustomerVO>> singleResponse =
                customerApiController.createCustomer(customerCreateVO);
        CustomerVO customerResponseVO = singleResponse.blockingGet().body();

        assertEquals(HttpStatus.CREATED,
                singleResponse.blockingGet().getStatus(),
                "A customer response with status CREATED is expected.");
        assertTrue(singleResponse.blockingGet().getBody().isPresent(),
                "A customer response contains object");
        assertEquals(customerCreateVO.getName(),
                customerResponseVO.getName(),
                "Returned customer should have equal name compared to the created one");
    }

    @DisplayName("Customer received")
    @Test
    void testReceiveCustomer() {
        CustomerVO customerVO = new CustomerVO();
        customerVO.setName("Customer Name");
        customerVO.setId("id1");
        Customer customer = tmForumMapper.map(customerVO);

        // Stub customer repo receive
        when(customerRepository.getCustomer("id1"))
                .thenReturn(Maybe.just(customer));

        // Retrieve customer
        HttpResponse<CustomerVO> customerVOHttpResponse =
                customerApiController.retrieveCustomer("id1", null).blockingGet();
        assertEquals(HttpStatus.OK,
                customerVOHttpResponse.getStatus(),
                "A customer response has OK status");
        assertTrue(customerVOHttpResponse.getBody().isPresent(), "A customer response has object body.");
        assertEquals(customerVO,
                customerVOHttpResponse.getBody().get(),
                "The same customer was returned");

    }
}
