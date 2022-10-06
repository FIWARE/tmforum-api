package org.fiware.tmforum.customer.repository;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.fiware.tmforum.common.repository.NgsiLdBaseRepository;
import org.fiware.tmforum.customer.domain.customer.Customer;
import org.fiware.tmforum.customer.exception.CustomerDeletionException;
import org.fiware.tmforum.customer.exception.CustomerExceptionReason;
import org.fiware.tmforum.customer.exception.CustomerListException;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.JavaObjectMapper;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class CustomerRepository extends NgsiLdBaseRepository {

    private final EntityVOMapper entityVOMapper;
    private final NGSIMapper ngsiMapper;
    private final JavaObjectMapper javaObjectMapper;

    public CustomerRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi, EntityVOMapper entityVOMapper, NGSIMapper ngsiMapper, JavaObjectMapper javaObjectMapper) {
        super(generalProperties, entitiesApi);
        this.entityVOMapper = entityVOMapper;
        this.ngsiMapper = ngsiMapper;
        this.javaObjectMapper = javaObjectMapper;
    }

    public Mono<Void> createCustomer(Customer customer) {
        return createEntity(javaObjectMapper.toEntityVO(customer), generalProperties.getTenant());
    }

    public Mono<Void> deleteCustomer(URI id) {
        return entitiesApi.removeEntityById(id, generalProperties.getTenant(), null)
                .onErrorResume(t -> {
                    if (t instanceof HttpClientResponseException e && e.getStatus().equals(HttpStatus.NOT_FOUND)) {
                        throw new CustomerDeletionException(String.format("Was not able to delete %s, since it does not exist.", id),
                                CustomerExceptionReason.NOT_FOUND);
                    }
                    throw new CustomerDeletionException(String.format("Was not able to delete %s.", id),
                            t,
                            CustomerExceptionReason.UNKNOWN);
                });
    }

    public Mono<List<Customer>> findCustomers(Integer offset, Integer limit) {
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
                        limit,
                        offset,
                        null,
                        getLinkHeader())
                .map(List::stream)
                .flatMap(entityVOStream -> zipToList(entityVOStream, Customer.class))
                .onErrorResume(t -> {
                    throw new CustomerListException("Was not able to list customers.", t);
                });
    }

    public <T> Mono<Void> updateCustomer(String id, T customer) {
        return patchEntity(URI.create(id), ngsiMapper.map(javaObjectMapper.toEntityVO(customer)));
    }

    public Mono<Customer> getCustomer(URI id) {
        return retrieveEntityById(id)
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, Customer.class));
    }

    private <T> Mono<List<T>> zipToList(Stream<EntityVO> entityVOStream, Class<T> targetClass) {
        return Mono.zip(
                entityVOStream.map(entityVO -> entityVOMapper.fromEntityVO(entityVO, targetClass)).toList(),
                oList -> Arrays.stream(oList).map(targetClass::cast).toList()
        );
    }
}
