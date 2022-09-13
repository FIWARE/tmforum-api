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

        /**
         * Validate references
         *
         * TODO: Need to check if and how to validate:
         * - PaymentMethodRef
         * - AgreementRef
         * - AccountRef
         * --> also check if these should extend RefEntity and what are allowed ref types in this case
         *
         * TODO: Check if relatedParty and engagedParty object is correct
         * - what are allowed ref types?
         */
        try {
            if (customer.getRelatedParty() != null && !customer.getRelatedParty().isEmpty()) {
                checkingSingle = validationService.getCheckingSingleOrThrow(customer.getRelatedParty(), customer);
                customerSingle = Single.zip(customerSingle, checkingSingle, (p1, p2) -> p1);
            }
            if (customer.getEngagedParty() != null) {
                checkingSingle =
                        validationService.getCheckingSingleOrThrow(
                                List.of(customer.getEngagedParty()),
                                customer);
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
        return customerRepository.deleteCustomer(id).toSingleDefault(HttpResponse.noContent());
    }

    @Override
    public Single<HttpResponse<List<CustomerVO>>> listCustomer(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return customerRepository.findCustomers()
                .map(List::stream)
                .map(customerStream -> customerStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Single<HttpResponse<CustomerVO>> patchCustomer(String id, CustomerUpdateVO customer) {
        // TODO: implement proper patch
        return null;
    }

    @Override
    public Single<HttpResponse<CustomerVO>> retrieveCustomer(String id, @Nullable String fields) {
        return customerRepository.getCustomer(id)
                .map(tmForumMapper::map)
                .toSingle()
                .map(HttpResponse::ok);
    }
}
