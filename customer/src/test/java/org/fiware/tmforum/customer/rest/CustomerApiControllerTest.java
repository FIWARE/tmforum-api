package org.fiware.tmforum.customer.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

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

    @DisplayName("Customer created")
    @Test
    void testCreateCustomer() {
        CustomerCreateVO customerCreateVO = new CustomerCreateVO();

        Single<HttpResponse<CustomerVO>> singleResponse = customerApiController.createCustomer(customerCreateVO);
    }
}
