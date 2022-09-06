package org.fiware.tmforum.customer.repository;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.fiware.ngsi.api.EntitiesApi;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.repository.NgsiLdBaseRepository;
import org.fiware.tmforum.customer.domain.customer.Customer;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.JavaObjectMapper;

import javax.inject.Singleton;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class CustomerRepository extends NgsiLdBaseRepository {

    private final EntityVOMapper entityVOMapper;
    private final JavaObjectMapper javaObjectMapper;

    public CustomerRepository(GeneralProperties generalProperties, EntitiesApi entitiesApi, EntityVOMapper entityVOMapper, JavaObjectMapper javaObjectMapper) {
        super(generalProperties, entitiesApi);
        this.entityVOMapper = entityVOMapper;
        this.javaObjectMapper = javaObjectMapper;
    }

    public Completable createCustomer(Customer customer) {
        return createEntity(javaObjectMapper.toEntityVO(customer), generalProperties.getTenant());
    }

    public Completable deleteCustomer(String id) {
        return entitiesApi.removeEntityById(URI.create(id), generalProperties.getTenant(), null);
    }

    public Single<List<Customer>> findCustomers() {
        return entitiesApi.queryEntities(generalProperties.getTenant(),
                null,
                null,
                Customer.TYPE_CUSTOMER,
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
                .flatMap(entityVOStream -> zipToList(entityVOStream, Customer.class));
    }

    public Maybe<Customer> getCustomer(String id) {
        return retrieveEntityById(URI.create(id))
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, Customer.class).toMaybe());
    }

    private <T> Single<List<T>> zipToList(Stream<EntityVO> entityVOStream, Class<T> targetClass) {
        return Single.zip(
                entityVOStream.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, targetClass)).toList(),
                oList -> Arrays.stream(oList).map(targetClass::cast).toList()
        );
    }
}
