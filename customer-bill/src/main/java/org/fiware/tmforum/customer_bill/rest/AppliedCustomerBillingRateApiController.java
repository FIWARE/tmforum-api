package org.fiware.tmforum.customer_bill.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customer_bill.api.AppliedCustomerBillingRateApi;
import org.fiware.customer_bill.model.AppliedCustomerBillingRateVO;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customer_bill.TMForumMapper;
import org.fiware.tmforum.customer_bill.repository.AppliedCustomerBillingRateRepository;

import javax.annotation.Nullable;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AppliedCustomerBillingRateApiController implements AppliedCustomerBillingRateApi {

    private final TMForumMapper tmForumMapper;
    private final AppliedCustomerBillingRateRepository billingRateRepository;
    private final ReferenceValidationService validationService;


    @Override
    public Single<HttpResponse<List<AppliedCustomerBillingRateVO>>> listAppliedCustomerBillingRate(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return billingRateRepository.findAppliedCustomerBillingRates()
                .map(List::stream)
                .map(customerBillStream -> customerBillStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Single<HttpResponse<AppliedCustomerBillingRateVO>> retrieveAppliedCustomerBillingRate(String id, @Nullable String fields) {
        return billingRateRepository.getAppliedCustomerBillingRate(id)
                .map(tmForumMapper::map)
                .toSingle()
                .map(HttpResponse::ok);
    }
}
