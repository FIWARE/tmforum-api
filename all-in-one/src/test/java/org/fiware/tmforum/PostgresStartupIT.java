package org.fiware.tmforum;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.fiware.tmforum.common.health.NgsiLdBrokerReadinessIndicator;
import org.fiware.tmforum.common.notification.EventConstants;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.storage.postgresql.PostgresReadinessIndicator;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The all-in-one app, every API included, starts, gets ready and serves lists on PostgreSQL with no broker reachable,
 * and leaves the broker-only NGSI-LD callback unrouted.
 */
@MicronautTest(environments = PostgresStorage.ENVIRONMENT, contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = "micronaut.http.services.ngsi.url", value = PostgresStartupIT.UNREACHABLE_BROKER)
@Property(name = "micronaut.http.services.ngsihealth.url", value = PostgresStartupIT.UNREACHABLE_BROKER)
@Property(name = "general.contextUrl", value = PostgresStartupIT.UNREACHABLE_BROKER + "/context.jsonld")
class PostgresStartupIT {

    // port 1 (tcpmux) is never served in the test environment, so every broker call fails fast
    static final String UNREACHABLE_BROKER = "http://localhost:1";
    private static final String READINESS_PATH = "/health/readiness";
    // the callback's consumed type and a well-formed notification, so only a missing route can answer 404
    private static final String JSON_UTF8 = "application/json;charset=utf-8";
    private static final String NOTIFICATION = """
            {"id":"urn:ngsi-ld:Notification:1","type":"Notification","subscriptionId":"urn:ngsi-ld:Subscription:1",
            "notifiedAt":"2024-01-01T00:00:00Z","data":[{"id":"urn:ngsi-ld:catalog:1","type":"catalog"}]}""";

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

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "/tmf-api/party/v4",
            "/tmf-api/productCatalogManagement/v4",
            "/tmf-api/serviceInventory/v4",
            "/tmf-api/resourceInventoryManagement/v4"})
    void callbackIsUnroutedWithoutBroker(String basepath) {
        HttpRequest<String> notification = HttpRequest.POST(basepath + EventConstants.SUBSCRIPTION_CALLBACK_PATH,
                NOTIFICATION).contentType(JSON_UTF8);

        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class,
                () -> httpClient.toBlocking().exchange(notification, String.class));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
