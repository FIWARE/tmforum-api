package org.fiware.tmforum.customer_bill.rest;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customer_bill.api.CustomerBillApi;
import org.fiware.customer_bill.model.CustomerBillUpdateVO;
import org.fiware.customer_bill.model.CustomerBillVO;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.customer_bill.TMForumMapper;
import org.fiware.tmforum.customer_bill.repository.CustomerBillRepository;

import javax.annotation.Nullable;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CustomerBillApiController implements CustomerBillApi {

    private final TMForumMapper tmForumMapper;

    private final CustomerBillRepository customerBillRepository;

    private final ReferenceValidationService validationService;

    @Override
    public Single<HttpResponse<List<CustomerBillVO>>> listCustomerBill(@Nullable String fields, @Nullable Integer offset, @Nullable Integer limit) {
        return customerBillRepository.findCustomerBills()
                .map(List::stream)
                .map(customerBillStream -> customerBillStream.map(tmForumMapper::map).toList())
                .map(HttpResponse::ok);
    }

    @Override
    public Single<HttpResponse<CustomerBillVO>> patchCustomerBill(String id, CustomerBillUpdateVO customerBill) {
        // TODO: implement proper patch
        return null;
    }

    @Override
    public Single<HttpResponse<CustomerBillVO>> retrieveCustomerBill(String id, @Nullable String fields) {
        return customerBillRepository.getCustomerBill(id)
                .map(tmForumMapper::map)
                .toSingle()
                .map(HttpResponse::ok);
    }
}
