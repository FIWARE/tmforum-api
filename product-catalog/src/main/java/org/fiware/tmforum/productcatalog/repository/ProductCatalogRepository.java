package org.fiware.tmforum.productcatalog.repository;

import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.fiware.tmforum.common.repository.NgsiLdBaseRepository;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.JavaObjectMapper;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.fiware.tmforum.party.exception.PartyListException;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.productcatalog.exception.CatalogException;
import org.fiware.tmforum.productcatalog.exception.CatalogExceptionReason;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.net.URI;
import java.util.List;

/**
 * Repository implementation to serve as backend for the party-api
 */
@Singleton
public class ProductCatalogRepository extends NgsiLdBaseRepository {


    public ProductCatalogRepository(GeneralProperties generalProperties, EntitiesApiClient entitiesApi, EntityVOMapper entityVOMapper, NGSIMapper ngsiMapper, JavaObjectMapper javaObjectMapper) {
        super(generalProperties, entitiesApi, javaObjectMapper, ngsiMapper, entityVOMapper);
    }

    public Mono<Catalog> getCatalog(URI id) {
        return retrieveEntityById(id)
                .flatMap(entityVO -> entityVOMapper.fromEntityVO(entityVO, Catalog.class));
    }


    public Mono<List<Catalog>> findCatalogs(Integer offset, Integer limit) {
        return entitiesApi.queryEntities(generalProperties.getTenant(),
                        null,
                        null,
                        Catalog.TYPE_CATALOGUE,
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
                .flatMap(entityVOStream -> zipToList(entityVOStream, Catalog.class))
                .onErrorResume(t -> {
                    throw new CatalogException("Was not able to list parties.", t, CatalogExceptionReason.UNKNOWN);
                });
    }

}
