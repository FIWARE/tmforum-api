package org.fiware.tmforum.customermanagement.repository;

import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.fiware.tmforum.common.repository.NgsiLdBaseRepository;
import org.fiware.tmforum.customermanagement.exception.CustomerManagementExceptionReason;
import org.fiware.tmforum.customermanagement.exception.CustomerManagementException;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.JavaObjectMapper;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.net.URI;
import java.util.List;

@Singleton
public class CustomerManagementRepository extends NgsiLdBaseRepository {

    public CustomerManagementRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi, EntityVOMapper entityVOMapper, NGSIMapper ngsiMapper, JavaObjectMapper javaObjectMapper) {
        super(generalProperties, entitiesApi, javaObjectMapper, ngsiMapper, entityVOMapper);
    }

    public <T> Mono<T> get(URI id, Class<T> entityClass) {
        return retrieveEntityById(id)
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, entityClass));
    }

    public <T> Mono<List<T>> findEntities(Integer offset, Integer limit, String entityType, Class<T> entityClass) {
        return entitiesApi.queryEntities(generalProperties.getTenant(),
                        null,
                        null,
                        entityType,
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
                .flatMap(entityVOStream -> zipToList(entityVOStream, entityClass))
                .onErrorResume(t -> {
                    throw new CustomerManagementException("Was not able to list entities.", t, CustomerManagementExceptionReason.UNKNOWN);
                });
    }

}
