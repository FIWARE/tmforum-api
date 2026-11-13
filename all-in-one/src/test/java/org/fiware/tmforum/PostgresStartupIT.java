package org.fiware.tmforum;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.fiware.tmforum.common.health.NgsiLdBrokerReadinessIndicator;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.storage.postgresql.PostgresReadinessIndicator;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The all-in-one app, every API included, starts, gets ready and serves lists on PostgreSQL with no broker reachable.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT, contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = "micronaut.http.services.ngsi.url", value = PostgresStartupIT.UNREACHABLE_BROKER)
@Property(name = "micronaut.http.services.ngsihealth.url", value = PostgresStartupIT.UNREACHABLE_BROKER)
@Property(name = "general.contextUrl", value = PostgresStartupIT.UNREACHABLE_BROKER + "/context.jsonld")
class PostgresStartupIT {

    // port 1 (tcpmux) is never served in the test environment, so every broker call fails fast
    static final String UNREACHABLE_BROKER = "http://localhost:1";
    private static final String READINESS_PATH = "/health/readiness";

    @Inject
    @Client("/")
    private HttpClient httpClient;

    @Inject
    private BeanContext beanContext;

    @Test
    void readyWithoutBroker() {
        assertTrue(beanContext.containsBean(PostgresReadinessIndicator.class), "no database readiness check");
        assertFalse(beanContext.containsBean(NgsiLdBrokerReadinessIndicator.class), "broker readiness check active");

        HttpResponse<String> readiness = httpClient.toBlocking().exchange(READINESS_PATH, String.class);

        assertEquals(HttpStatus.OK, readiness.getStatus());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "/tmf-api/partyRole/v4/partyRole",
            "/tmf-api/party/v4/organization",
            "/tmf-api/productCatalogManagement/v4/catalog",
            "/tmf-api/resourceInventoryManagement/v4/resource"})
    void servesListsWithoutBroker(String listPath) {
        assertEquals(HttpStatus.OK, httpClient.toBlocking().exchange(listPath, String.class).getStatus());
    }
}
