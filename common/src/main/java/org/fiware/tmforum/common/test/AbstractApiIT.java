package org.fiware.tmforum.common.test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.junit.jupiter.api.BeforeEach;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Common super class for the api tests
 */
public abstract class AbstractApiIT {

    private final EntitiesApiClient entitiesApiClient;
    private final GeneralProperties generalProperties;
    protected final ObjectMapper objectMapper;

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
                null,
                1000,
                0,
                false,
                null,
                getLinkHeader(generalProperties.getContextUrl()))
                .map(HttpResponse::body)
                .block();
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

    /**
     * Asserts equality of the JSON representations, ignoring empty arrays: the mapping library does not store an
     * empty list in the broker, so a list sent as {@code []} reads back as absent.
     */
    protected void assertEqualsIgnoringEmptyLists(Object expected, Object actual, String message) {
        assertEquals(withoutEmptyArrays(objectMapper.valueToTree(expected)),
                withoutEmptyArrays(objectMapper.valueToTree(actual)), message);
    }

    private static JsonNode withoutEmptyArrays(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node instanceof ObjectNode objectNode) {
            List<String> emptyArrays = objectNode.properties().stream()
                    .filter(property -> property.getValue().isArray() && property.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .toList();
            objectNode.remove(emptyArrays);
        }
        node.forEach(AbstractApiIT::withoutEmptyArrays);
        return node;
    }

    protected String getLinkHeader(URL contextUrl) {
        return String.format("<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json",
                contextUrl);
    }
}
