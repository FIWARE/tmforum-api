package org.fiware.tmforum.customerbillmanagement.rest;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customerbillmanagement.api.AppliedCustomerBillingRateApi;
import org.fiware.customerbillmanagement.api.ext.AppliedCustomerBillingRateExtensionApi;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateCreateVO;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateUpdateVO;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.fiware.tmforum.common.rest.AbstractApiController;
import org.fiware.tmforum.common.validation.ReferenceValidationService;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.customerbillmanagement.TMForumMapper;
import org.fiware.tmforum.customerbillmanagement.domain.AppliedCustomerBillingRate;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller("${general.basepath:/}")
@Requires(property = "apiExtension.enabled", value = "true")
public class ExtendedAppliedCustomerBillingRateApiController extends AbstractApiController<AppliedCustomerBillingRate> implements AppliedCustomerBillingRateExtensionApi {

    private final TMForumMapper tmForumMapper;
    private final Clock clock;

    public ExtendedAppliedCustomerBillingRateApiController(
            QueryParser queryParser,
            ReferenceValidationService validationService,
            TmForumRepository repository, TMForumMapper tmForumMapper, TMForumEventHandler eventHandler, Clock clock) {
        super(queryParser, validationService, repository, eventHandler);
        this.tmForumMapper = tmForumMapper;
        this.clock = clock;
    }

    @Override
    public Mono<HttpResponse<AppliedCustomerBillingRateVO>> createAppliedCustomerBillingRate(AppliedCustomerBillingRateCreateVO appliedCustomerBillingRateCreate) {

        if (getNullSafeBoolean(appliedCustomerBillingRateCreate.getIsBilled()) && appliedCustomerBillingRateCreate.getBill() == null) {
            throw new TmForumException("If an AppliedCustomerBillingRate is billed, the bill needs to be included.", TmForumExceptionReason.INVALID_DATA);
        }

        if (!getNullSafeBoolean(appliedCustomerBillingRateCreate.getIsBilled()) && appliedCustomerBillingRateCreate.getBillingAccount() == null) {
            throw new TmForumException("If an AppliedCustomerBillingRate is not yet billed, the billing account needs to be included.", TmForumExceptionReason.INVALID_DATA);
        }

        AppliedCustomerBillingRate appliedCustomerBillingRate = tmForumMapper.map(
                tmForumMapper.map(appliedCustomerBillingRateCreate, IdHelper.toNgsiLd(UUID.randomUUID().toString(), AppliedCustomerBillingRate.TYPE_APPLIED_CUSTOMER_BILLING_RATE)));
        appliedCustomerBillingRate.setDate(clock.instant());

        return create(getCheckingMono(appliedCustomerBillingRate), AppliedCustomerBillingRate.class)
                .map(tmForumMapper::map)
                .map(HttpResponse::created);
    }

    private Mono<AppliedCustomerBillingRate> getCheckingMono(AppliedCustomerBillingRate appliedCustomerBillingRate) {
        List<List<? extends ReferencedEntity>> references = new ArrayList<>();
        Optional.ofNullable(appliedCustomerBillingRate.getBill()).ifPresent(bill -> references.add(List.of(bill)));
        Optional.ofNullable(appliedCustomerBillingRate.getBillingAccount()).ifPresent(billingAccount -> references.add(List.of(billingAccount)));
        Optional.ofNullable(appliedCustomerBillingRate.getProduct()).ifPresent(productRef -> references.add(List.of(productRef)));
        return getCheckingMono(appliedCustomerBillingRate, references)
                .onErrorMap(throwable -> new TmForumException(
                        String.format("Was not able to create appliedCustomerBillingRate %s", appliedCustomerBillingRate.getId()), throwable,
                        TmForumExceptionReason.INVALID_RELATIONSHIP));
    }

    @Override
    public Mono<HttpResponse<AppliedCustomerBillingRateVO>> updateAppliedCustomerBillingRate(String id, AppliedCustomerBillingRateUpdateVO appliedCustomerBillingRateUdpate) {
        // non-ngsi-ld ids cannot exist.
        if (!IdHelper.isNgsiLdId(id)) {
            throw new TmForumException("Did not receive a valid id, such appliedCustomerBillingRate cannot exist.",
                    TmForumExceptionReason.NOT_FOUND);
        }

        AppliedCustomerBillingRate updatedCustomerBillingRate = tmForumMapper.map(tmForumMapper.map(appliedCustomerBillingRateUdpate, id));

        return retrieve(id, AppliedCustomerBillingRate.class)
                .doOnSuccess(appliedCustomerBillingRate -> {
                    if (getNullSafeBoolean(appliedCustomerBillingRateUdpate.getIsBilled())
                            && appliedCustomerBillingRateUdpate.getBill() == null
                            && appliedCustomerBillingRate.getBill() == null) {
                        throw new TmForumException("If an AppliedCustomerBillingRate is billed, the bill needs to be included.", TmForumExceptionReason.INVALID_DATA);
                    }
                    if (!getNullSafeBoolean(appliedCustomerBillingRateUdpate.getIsBilled())
                            && appliedCustomerBillingRateUdpate.getBillingAccount() == null
                            && appliedCustomerBillingRate.getBillingAccount() == null) {
                        throw new TmForumException("If an AppliedCustomerBillingRate is not yet billed, the billing account needs to be included.", TmForumExceptionReason.INVALID_DATA);
                    }
                })
                .flatMap(acbr -> patch(id, updatedCustomerBillingRate, getCheckingMono(updatedCustomerBillingRate), AppliedCustomerBillingRate.class))
                .map(tmForumMapper::map)
                .map(HttpResponse::ok);
    }

    @Override
    public Mono<HttpResponse<Object>> deleteAppliedCustomerBill(String id) {
        return delete(id);
    }

    private boolean getNullSafeBoolean(Boolean booleanValue) {
        return Optional.ofNullable(booleanValue).orElse(false);
    }
}
