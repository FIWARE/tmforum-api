package org.fiware.tmforum.customer.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customer.api.CustomerApi;
import org.fiware.customer.model.CustomerCreateVO;
import org.fiware.customer.model.CustomerUpdateVO;
import org.fiware.customer.model.CustomerVO;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customer.TMForumMapper;
import org.fiware.tmforum.customer.domain.customer.Customer;
import org.fiware.tmforum.customer.exception.CustomerCreationException;
import org.fiware.tmforum.customer.exception.CustomerDeletionException;
import org.fiware.tmforum.customer.exception.CustomerExceptionReason;
import org.fiware.tmforum.customer.exception.CustomerUpdateException;
import org.fiware.tmforum.customer.repository.CustomerRepository;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.fiware.tmforum.common.CommonConstants.DEFAULT_LIMIT;
import static org.fiware.tmforum.common.CommonConstants.DEFAULT_OFFSET;

@Slf4j
@Controller("${general.basepath:/}")
@RequiredArgsConstructor
public class CustomerApiController implements CustomerApi {

    private final TMForumMapper tmForumMapper;
    private final CustomerRepository customerRepository;
    private final ReferenceValidationService validationService;

    @Override
    public Mono<HttpResponse<CustomerVO>> createCustomer(CustomerCreateVO customerCreateVO) {
        CustomerVO customerVO = tmForumMapper.map(customerCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Customer.TYPE_CUSTOMER));
        Customer customer = tmForumMapper.map(customerVO);

        Mono<Customer> customerMono = Mono.just(customer);
        Mono<Customer> checkingMono;

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
        if (customer.getRelatedParty() != null && !customer.getRelatedParty().isEmpty()) {
            checkingMono = validationService.getCheckingMono(customer.getRelatedParty(), customer)
                    .onErrorMap(throwable -> new CustomerCreationException(String.format("Was not able to create customer %s", customer.getId()), throwable, CustomerExceptionReason.INVALID_RELATIONSHIP));
            customerMono = Mono.zip(customerMono, checkingMono, (p1, p2) -> p1);
        }
        if (customer.getEngagedParty() != null) {
            checkingMono =
                    validationService.getCheckingMono(
                                    List.of(customer.getEngagedParty()),
                                    customer)
                            .onErrorMap(throwable -> new CustomerCreationException(String.format("Was not able to create customer %s", customer.getId()), throwable, CustomerExceptionReason.INVALID_RELATIONSHIP));
            customerMono = Mono.zip(customerMono, checkingMono, (p1, p2) -> p1);
        }

        return customerMono
                .flatMap(customerToCreate -> customerRepository.createCustomer(customerToCreate).then(Mono.just(customerToCreate)))
                .onErrorMap(t -> {
                    if (t instanceof HttpClientResponseException e) {
                        return switch (e.getStatus()) {
                            case CONFLICT ->
                                    new CustomerCreationException(String.format("Conflict on creating the customer: %s", e.getMessage()), CustomerExceptionReason.CONFLICT);
                            case BAD_REQUEST ->
                                    new CustomerCreationException(String.format("Did not receive a valid customer: %s.", e.getMessage()), CustomerExceptionReason.INVALID_DATA);
                            default ->
                                    new CustomerCreationException(String.format("Unspecified downstream error: %s", e.getMessage()), CustomerExceptionReason.UNKNOWN);
                        };
                    } else {
                        return t;
                    }
                })
                .cast(Customer.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    @Override
    public Mono<HttpResponse<Object>> deleteCustomer(String id) {

        if (!IdHelper.isNgsiLdId(id)) {
            throw new CustomerDeletionException("Did not receive a valid id, such organization cannot exist.", CustomerExceptionReason.NOT_FOUND);
        }

        return customerRepository.deleteCustomer(URI.create(id)).then(Mono.just(HttpResponse.noContent()));
    }

    @Override
    public Mono<HttpResponse<List<CustomerVO>>> listCustomer(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        offset = Optional.ofNullable(offset).orElse(DEFAULT_OFFSET);
        limit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);

        return customerRepository.findCustomers(offset, limit)
                .map(List::stream)
                .map(customerStream -> customerStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<CustomerVO>> patchCustomer(String id, CustomerUpdateVO customerUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new CustomerUpdateException("Did not receive a valid id, such organization cannot exist.", CustomerExceptionReason.NOT_FOUND);
        }

        Customer updatedCustomer = tmForumMapper.map(tmForumMapper.map(customerUpdateVO, id));

        URI idUri = URI.create(id);
        return customerRepository.updateCustomer(id, updatedCustomer)
                .then(customerRepository.getCustomer(idUri))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<CustomerVO>> retrieveCustomer(String id, @Nullable String fields) {

        if (!IdHelper.isNgsiLdId(id)) {
            return Mono.just(HttpResponse.notFound());
        }

        return customerRepository.getCustomer(URI.create(id))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok)
                .switchIfEmpty(Mono.just(HttpResponse.notFound()))
                .map(HttpResponse.class::cast);
    }
}
