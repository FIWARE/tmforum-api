package org.fiware.tmforum.customer.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customer.api.CustomerApi;
import org.fiware.customer.model.CustomerCreateVO;
import org.fiware.customer.model.CustomerUpdateVO;
import org.fiware.customer.model.CustomerVO;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customer.TMForumMapper;
import org.fiware.tmforum.customer.domain.customer.Customer;
import org.fiware.tmforum.customer.exception.CustomerCreationException;
import org.fiware.tmforum.customer.repository.CustomerRepository;

import javax.annotation.Nullable;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CustomerApiController implements CustomerApi {

    private final TMForumMapper tmForumMapper;
    private final CustomerRepository customerRepository;
    private final ReferenceValidationService validationService;

    @Override
    public Single<HttpResponse<CustomerVO>> createCustomer(CustomerCreateVO customerCreateVO) {
        CustomerVO customerVO = tmForumMapper.map(customerCreateVO);
        Customer customer = tmForumMapper.map(customerVO);

        Single<Customer> customerSingle = Single.just(customer);
        Single<Customer> checkingSingle;

        try {
            if (customer.getRelatedParty() != null && !customer.getRelatedParty().isEmpty()) {
                checkingSingle = validationService.getCheckingSingleOrThrow(customer.getRelatedParty(), customer);
                customerSingle = Single.zip(customerSingle, checkingSingle, (p1, p2) -> p1);
            }
        } catch (NonExistentReferenceException e) {
            throw new CustomerCreationException(String.format("Was not able to create customer %s", customer.getId()), e);
        }

        return customerSingle
                .flatMap(customerToCreate -> customerRepository.createCustomer(customerToCreate).toSingleDefault(customerToCreate))
                .cast(Customer.class)
                .map(tmForumMapper::map)
                .subscribeOn(Schedulers.io())
                .map(HttpResponse::created);
    }

    @Override
    public Single<HttpResponse<Object>> deleteCustomer(String id) {
        return null;
    }

    @Override
    public Single<HttpResponse<List<CustomerVO>>> listCustomer(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return null;
    }

    @Override
    public Single<HttpResponse<CustomerVO>> patchCustomer(String id, CustomerUpdateVO customer) {
        return null;
    }

    @Override
    public Single<HttpResponse<CustomerVO>> retrieveCustomer(String id, @Nullable String fields) {
        return null;
    }
}
