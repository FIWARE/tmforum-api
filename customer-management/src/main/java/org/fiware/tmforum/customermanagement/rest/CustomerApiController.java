package org.fiware.tmforum.customermanagement.rest;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.fiware.customermanagement.api.CustomerApi;
import org.fiware.customermanagement.model.CustomerCreateVO;
import org.fiware.customermanagement.model.CustomerUpdateVO;
import org.fiware.customermanagement.model.CustomerVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.customermanagement.TMForumMapper;
import org.fiware.tmforum.customermanagement.domain.Customer;
import org.fiware.tmforum.customermanagement.exception.CustomerManagementException;
import org.fiware.tmforum.customermanagement.exception.CustomerManagementExceptionReason;
import org.fiware.tmforum.customermanagement.repository.CustomerManagementRepository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
public class CustomerApiController extends AbstractApiController implements CustomerApi {

    public CustomerApiController(TMForumMapper tmForumMapper, ReferenceValidationService validationService, CustomerManagementRepository customerManagementRepository) {
        super(tmForumMapper, validationService, customerManagementRepository);
    }

    @Override
    public Mono<HttpResponse<CustomerVO>> createCustomer(@NonNull CustomerCreateVO customerCreateVO) {
        Customer customer = tmForumMapper.map(
                tmForumMapper.map(customerCreateVO, IdHelper.toNgsiLd(UUID.randomUUID().toString(), Customer.TYPE_CUSTOMER)));

        return create(getCheckingMono(customer), Customer.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<Customer> getCheckingMono(Customer customer) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        references.add(customer.getAccount());
        references.add(customer.getAgreement());
        references.add(customer.getPaymentMethod());
        references.add(customer.getRelatedParty());

        Optional.ofNullable(customer.getEngagedParty()).map(List::of).ifPresent(references::add);

        return getCheckingMono(customer, references)
                .onErrorMap(throwable ->
                        new CustomerManagementException(
                                String.format("Was not able to create customer %s", customer.getId()),
                                throwable,
                                CustomerManagementExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<Object>> deleteCustomer(@NonNull String id) {
        return delete(id);
    }

    @Override
    public Mono<HttpResponse<List<CustomerVO>>> listCustomer(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return list(offset, limit, Customer.TYPE_CUSTOMER, Customer.class)
                .map(serviceCategoryStream -> serviceCategoryStream.map(tmForumMapper::map).toList())
                .switchIfEmpty(Mono.just(List.of()))
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<CustomerVO>> patchCustomer(@NonNull String id, @NonNull CustomerUpdateVO customerUpdateVO) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new CustomerManagementException("Did not receive a valid id, such customer cannot exist.", CustomerManagementExceptionReason.NOT_FOUND);
        }
        Customer customer = tmForumMapper.map(customerUpdateVO, id);

        return patch(id, customer, getCheckingMono(customer), Customer.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<CustomerVO>> retrieveCustomer(@NonNull String id, @Nullable String fields) {
        return retrieve(id, Customer.class)
                .switchIfEmpty(Mono.error(new CustomerManagementException("No such customer exists.", CustomerManagementExceptionReason.NOT_FOUND)))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }
}
