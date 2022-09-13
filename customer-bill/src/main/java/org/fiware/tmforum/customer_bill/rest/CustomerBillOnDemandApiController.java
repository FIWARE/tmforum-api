package org.fiware.tmforum.customer_bill.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customer_bill.api.CustomerBillOnDemandApi;
import org.fiware.customer_bill.model.CustomerBillOnDemandCreateVO;
import org.fiware.customer_bill.model.CustomerBillOnDemandVO;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customer_bill.TMForumMapper;
import org.fiware.tmforum.customer_bill.domain.customer_bill.CustomerBillOnDemand;
import org.fiware.tmforum.customer_bill.exception.CustomerBillOnDemandCreationException;
import org.fiware.tmforum.customer_bill.repository.CustomerBillOnDemandRepository;

import javax.annotation.Nullable;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CustomerBillOnDemandApiController implements CustomerBillOnDemandApi {

    private final TMForumMapper tmForumMapper;
    private final CustomerBillOnDemandRepository customerBillOnDemandRepository;
    private final ReferenceValidationService validationService;


    @Override
    public Single<HttpResponse<CustomerBillOnDemandVO>> createCustomerBillOnDemand(CustomerBillOnDemandCreateVO customerBillOnDemandCreateVO) {

        CustomerBillOnDemandVO customerBillOnDemandVO = tmForumMapper.map(customerBillOnDemandCreateVO);
        CustomerBillOnDemand customerBillOnDemand = tmForumMapper.map(customerBillOnDemandVO);

        Single<CustomerBillOnDemand> customerBillOnDemandSingle = Single.just(customerBillOnDemand);
        Single<CustomerBillOnDemand> checkingSingle;

        /**
         * Validate references
         *
         * TODO: Need to check if and how to validate:
         * - BillRef
         * - BillingAccountRef
         * --> also check if these should extend RefEntity and what are allowed ref types in this case
         *
         * TODO: Check if relatedParty object is correct
         * - what are allowed ref types?
         */
        try {
            if (customerBillOnDemand.getRelatedParty() != null) {
                checkingSingle =
                        validationService.getCheckingSingleOrThrow(
                                List.of(customerBillOnDemand.getRelatedParty()),
                                customerBillOnDemand);
                customerBillOnDemandSingle =
                        Single.zip(customerBillOnDemandSingle, checkingSingle, (p1, p2) -> p1);
            }
        } catch (NonExistentReferenceException e) {
            throw new CustomerBillOnDemandCreationException(
                    String.format("Was not able to create customer bill on demand %s",
                            customerBillOnDemand.getId()),
                    e);
        }

        return customerBillOnDemandSingle
                .flatMap(customerBillOnDemandToCreate -> customerBillOnDemandRepository.createCustomerBillOnDemand(customerBillOnDemandToCreate).toSingleDefault(customerBillOnDemandToCreate))
                .cast(CustomerBillOnDemand.class)
                .map(tmForumMapper::map)
                .subscribeOn(Schedulers.io())
                .map(HttpResponse::created);
    }

    @Override
    public Single<HttpResponse<List<CustomerBillOnDemandVO>>> listCustomerBillOnDemand(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return customerBillOnDemandRepository.findCustomerBillsOnDemand()
                .map(List::stream)
                .map(customerBillOnDemandStream -> customerBillOnDemandStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Single<HttpResponse<CustomerBillOnDemandVO>> retrieveCustomerBillOnDemand(String id, @Nullable String fields) {
        return customerBillOnDemandRepository.getCustomerBillOnDemand(id)
                .map(tmForumMapper::map)
                .toSingle()
                .map(HttpResponse::ok);
    }
}
