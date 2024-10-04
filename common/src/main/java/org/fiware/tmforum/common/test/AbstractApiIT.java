package org.fiware.tmforum.common.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wistefan.mapping.AdditionalPropertyMixin;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.junit.jupiter.api.BeforeEach;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Common super class for the api tests
 */
public abstract class AbstractApiIT {

    private final EntitiesApiClient entitiesApiClient;
    private final ObjectMapper objectMapper;
    private final GeneralProperties generalProperties;

    /**
     * Needs to return the type of the handled entities, to allow automated cleanup before each test.
     *
     * @return the entity type to be cleaned up
     */
    protected abstract String getEntityType();

    protected AbstractApiIT(EntitiesApiClient entitiesApiClient,
                            ObjectMapper objectMapper, GeneralProperties generalProperties) {
        this.entitiesApiClient = entitiesApiClient;
        this.objectMapper = objectMapper;
        this.generalProperties = generalProperties;
    }

    @BeforeEach
    public void cleanUp() {
        this.objectMapper
                .addMixIn(AdditionalPropertyVO.class, AdditionalPropertyMixin.class);
        this.objectMapper.findAndRegisterModules();
//        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        EntityListVO entityVOS = entitiesApiClient.queryEntities(null,
                null,
                null,
                getEntityType(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1000,
                0,
                null,
                getLinkHeader(generalProperties.getContextUrl())).block();
        entityVOS.stream()
                .filter(Objects::nonNull)
                .map(EntityVO::getId)
                .filter(Objects::nonNull)
                .forEach(eId -> entitiesApiClient.removeEntityById(eId, null, null).block());
    }

    // Helper method to catch potential http exceptions and return the status code.
    public <T> HttpResponse<T> callAndCatch(Callable<HttpResponse<T>> request) throws Exception {
        try {
            return request.call();
        } catch (HttpClientResponseException e) {
            return (HttpResponse<T>) e.getResponse();
        }
    }

    protected String getLinkHeader(URL contextUrl) {
        return String.format("<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json",
                contextUrl);
    }
}
