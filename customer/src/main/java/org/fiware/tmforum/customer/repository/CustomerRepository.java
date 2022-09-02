package org.fiware.tmforum.customer.repository;

import io.reactivex.Completable;
import org.fiware.ngsi.api.EntitiesApi;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.repository.NgsiLdBaseRepository;
import org.fiware.tmforum.customer.domain.customer.Customer;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.JavaObjectMapper;

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


}
