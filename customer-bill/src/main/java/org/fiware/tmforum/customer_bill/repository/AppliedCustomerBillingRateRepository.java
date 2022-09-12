package org.fiware.tmforum.customer_bill.repository;

import io.reactivex.Maybe;
import io.reactivex.Single;
import org.fiware.ngsi.api.EntitiesApi;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.repository.NgsiLdBaseRepository;
import org.fiware.tmforum.customer_bill.domain.customer_bill.AppliedCustomerBillingRate;
import org.fiware.tmforum.customer_bill.domain.customer_bill.CustomerBill;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.JavaObjectMapper;

import javax.inject.Singleton;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class AppliedCustomerBillingRateRepository extends NgsiLdBaseRepository {

    private final EntityVOMapper entityVOMapper;
    private final JavaObjectMapper javaObjectMapper;

    public AppliedCustomerBillingRateRepository(GeneralProperties generalProperties, EntitiesApi entitiesApi, EntityVOMapper entityVOMapper, JavaObjectMapper javaObjectMapper) {
        super(generalProperties, entitiesApi);
        this.entityVOMapper = entityVOMapper;
        this.javaObjectMapper = javaObjectMapper;
    }

    public Single<List<AppliedCustomerBillingRate>> findAppliedCustomerBillingRates() {
        return entitiesApi.queryEntities(generalProperties.getTenant(),
                        null,
                        null,
                        AppliedCustomerBillingRate.TYPE_APPLIED_CUSTOMER_BILLING_RATE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        getLinkHeader())
                .map(List::stream)
                .flatMap(entityVOStream -> zipToList(entityVOStream, AppliedCustomerBillingRate.class));
    }

    public Maybe<AppliedCustomerBillingRate> getAppliedCustomerBillingRate(String id) {
        return retrieveEntityById(URI.create(id))
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, AppliedCustomerBillingRate.class).toMaybe());
    }

    private <T> Single<List<T>> zipToList(Stream<EntityVO> entityVOStream, Class<T> targetClass) {
        return Single.zip(
                entityVOStream.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, targetClass)).toList(),
                oList -> Arrays.stream(oList).map(targetClass::cast).toList()
        );
    }

}
