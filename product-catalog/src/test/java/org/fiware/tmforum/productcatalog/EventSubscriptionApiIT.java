package org.fiware.tmforum.productcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.EventsSubscriptionApiTestClient;
import org.fiware.productcatalog.api.EventsSubscriptionApiTestSpec;
import org.fiware.productcatalog.model.EventSubscriptionInputVO;
import org.fiware.productcatalog.model.EventSubscriptionInputVOTestExample;
import org.fiware.productcatalog.model.EventSubscriptionVO;
import org.fiware.productcatalog.model.EventSubscriptionVOTestExample;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(packages = { "org.fiware.tmforum.productcatalog" })
public class EventSubscriptionApiIT extends AbstractApiIT implements EventsSubscriptionApiTestSpec {

    private static final String ANY_CALLBACK = "https://test.com";

    public final EventsSubscriptionApiTestClient eventsSubscriptionApiTestClient;
    private EventSubscriptionInputVO eventSubscriptionInputVO;
    private String message;
    private EventSubscriptionVO expectedEventSubscription;


    public EventSubscriptionApiIT(EventsSubscriptionApiTestClient eventsSubscriptionApiTestClient,
                                  EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
                                  GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.eventsSubscriptionApiTestClient = eventsSubscriptionApiTestClient;
    }

    @Override
    protected String getEntityType() {
        return Subscription.TYPE_SUBSCRIPTION;
    }

    @ParameterizedTest
    @MethodSource("provideValidEventSubscriptionInputs")
    public void registerListener201(String message, EventSubscriptionInputVO eventSubscriptionInputVO,
                                    EventSubscriptionVO expectedEventSubscription) throws Exception {
        this.message = message;
        this.eventSubscriptionInputVO = eventSubscriptionInputVO;
        this.expectedEventSubscription = expectedEventSubscription;
        registerListener201();
    }

    @Override
    public void registerListener201() throws Exception {
        HttpResponse<EventSubscriptionVO> registerResponse =
                callAndCatch(() -> eventsSubscriptionApiTestClient.registerListener(eventSubscriptionInputVO));
        assertEquals(HttpStatus.CREATED, registerResponse.getStatus(), message);
        assertTrue(registerResponse.getBody().isPresent());
        expectedEventSubscription.setId(registerResponse.getBody().get().getId());

        assertEquals(expectedEventSubscription, registerResponse.getBody().get(), message);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidEventSubscriptionInputs")
    public void registerListener400(String message, EventSubscriptionInputVO eventSubscriptionInputVO) throws Exception {
        this.message = message;
        this.eventSubscriptionInputVO = eventSubscriptionInputVO;
        registerListener400();
    }

    @Override
    public void registerListener400() throws Exception {
        HttpResponse<EventSubscriptionVO> registerResponse = callAndCatch(
                () -> eventsSubscriptionApiTestClient.registerListener(eventSubscriptionInputVO));
        assertEquals(HttpStatus.BAD_REQUEST, registerResponse.getStatus(), message);
        Optional<ErrorDetails> optionalErrorDetails = registerResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Override
    public void registerListener401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Override
    public void registerListener403() throws Exception {

    }

    @Disabled("Impossible status.")
    @Override
    public void registerListener404() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Override
    public void registerListener405() throws Exception {

    }

    @Test
    @Override
    public void registerListener409() throws Exception {
        eventSubscriptionInputVO = EventSubscriptionInputVOTestExample.build()
                .query("eventType=CatalogCreateEvent")
                .callback(ANY_CALLBACK);

        HttpResponse<EventSubscriptionVO> registerResponse =
                callAndCatch(() -> eventsSubscriptionApiTestClient.registerListener(eventSubscriptionInputVO));
        assertEquals(HttpStatus.CREATED, registerResponse.getStatus(), message);

        HttpResponse<EventSubscriptionVO> duplicatedListenerRegisterResponse =
                callAndCatch(() -> eventsSubscriptionApiTestClient.registerListener(eventSubscriptionInputVO));
        assertEquals(HttpStatus.CONFLICT, duplicatedListenerRegisterResponse.getStatus(), message);
    }

    @Override
    public void registerListener500() throws Exception {

    }

    @Override
    public void unregisterListener204() throws Exception {
        EventSubscriptionInputVO eventSubscriptionInputVO = EventSubscriptionInputVOTestExample.build()
                .query("eventType=CatalogCreateEvent")
                .callback(ANY_CALLBACK);
        HttpResponse<EventSubscriptionVO> eventSubscriptionVOHttpResponse =
                callAndCatch(() -> eventsSubscriptionApiTestClient.registerListener(eventSubscriptionInputVO));
        assertEquals(HttpStatus.CREATED, eventSubscriptionVOHttpResponse.getStatus(),
                "A listener with callback should have been created.");
        assertTrue(eventSubscriptionVOHttpResponse.getBody().isPresent());

        String hubId = eventSubscriptionVOHttpResponse.getBody().get().getId();
        HttpResponse<?> httpResponse =
                callAndCatch(() -> eventsSubscriptionApiTestClient.unregisterListener(hubId));
        assertEquals(HttpStatus.NO_CONTENT, httpResponse.getStatus(),
                "A listener with callback should have been deleted.");
    }

    @Override
    public void unregisterListener400() throws Exception {
        HttpResponse<?> httpResponse =
                callAndCatch(() -> eventsSubscriptionApiTestClient.unregisterListener(null));
        assertEquals(HttpStatus.BAD_REQUEST, httpResponse.getStatus(),
                "Should have returned 400 when ID is null");
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Override
    public void unregisterListener401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Override
    public void unregisterListener403() throws Exception {

    }

    @Test
    @Override
    public void unregisterListener404() throws Exception {
        HttpResponse<?> httpResponse =
                callAndCatch(() -> eventsSubscriptionApiTestClient.unregisterListener("non-existing-id"));
        assertEquals(HttpStatus.NOT_FOUND, httpResponse.getStatus(),
                "Should have return 404 when non-existing ID provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Override
    public void unregisterListener405() throws Exception {

    }

    @Override
    public void unregisterListener500() throws Exception {

    }

    private static Stream<Arguments> provideValidEventSubscriptionInputs() {
        List<Arguments> testEntries = new ArrayList<>();

        testEntries.add(
                Arguments.of("A listener with callback and simple query should have been created.",
                        EventSubscriptionInputVOTestExample.build()
                                .query("eventType=CatalogCreateEvent")
                                .callback(ANY_CALLBACK),
                        EventSubscriptionVOTestExample.build()
                                .query("eventType=CatalogCreateEvent")
                                .callback(ANY_CALLBACK)
                )
        );
        testEntries.add(
                Arguments.of("A listener with event types of multiple entities within module should have been created.",
                        EventSubscriptionInputVOTestExample.build()
                                .query("eventType=CatalogCreateEvent;eventType=CategoryCreateEvent")
                                .callback(ANY_CALLBACK),
                        EventSubscriptionVOTestExample.build()
                                .query("eventType=CatalogCreateEvent;eventType=CategoryCreateEvent")
                                .callback(ANY_CALLBACK)
                )
        );
        testEntries.add(
                Arguments.of("A listener with callback and complex query should have been created.",
                        EventSubscriptionInputVOTestExample.build()
                                .query("eventType=CatalogCreateEvent&event.catalog.lifecycleStatus=created")
                                .callback(ANY_CALLBACK),
                        EventSubscriptionVOTestExample.build()
                                .query("eventType=CatalogCreateEvent&event.catalog.lifecycleStatus=created")
                                .callback(ANY_CALLBACK)
                )
        );
        testEntries.add(
                Arguments.of("A listener with callback and complex query should have been created.",
                        EventSubscriptionInputVOTestExample.build()
                                .query("eventType=CatalogCreateEvent&event.catalog.lifecycleStatus=created" +
                                        "&fields=event.catalog.id,event.catalog.name")
                                .callback(ANY_CALLBACK),
                        EventSubscriptionVOTestExample.build()
                                .query("eventType=CatalogCreateEvent&event.catalog.lifecycleStatus=created" +
                                        "&fields=event.catalog.id,event.catalog.name")
                                .callback(ANY_CALLBACK)
                )
        );

        return testEntries.stream();
    }

    private static Stream<Arguments> provideInvalidEventSubscriptionInputs() {
        List<Arguments> testEntries = new ArrayList<>();

        testEntries.add(
                Arguments.of("A query with several ANDed event types should not be created.",
                        EventSubscriptionInputVOTestExample.build()
                                .query("eventType=CatalogCreateEvent&eventType=CatalogDeleteEvent")
                                .callback(ANY_CALLBACK)
                )
        );
        testEntries.add(
                Arguments.of("A query with different ORed section conditions should not be created.",
                        EventSubscriptionInputVOTestExample.build()
                                .query("eventType=CatalogCreateEvent;event.catalog.name=Some")
                                .callback(ANY_CALLBACK)
                )
        );
        testEntries.add(
                Arguments.of("A query with both logical operators AND and OR should not be created.",
                        EventSubscriptionInputVOTestExample.build()
                                .query("eventType=CatalogCreateEvent;eventType=CatalogDeleteEvent&event.catalog.name=Some")
                                .callback(ANY_CALLBACK)
                )
        );
        testEntries.add(
                Arguments.of("A query with entities from other modules should not be create.",
                        EventSubscriptionInputVOTestExample.build()
                                .query("eventType=ProductCreateEvent")
                                .callback(ANY_CALLBACK)
                )
        );

        return testEntries.stream();
    }
}
