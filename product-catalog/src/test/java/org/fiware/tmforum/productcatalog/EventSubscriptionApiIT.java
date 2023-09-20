package org.fiware.tmforum.productcatalog;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.productcatalog.api.EventsSubscriptionApiTestClient;
import org.fiware.productcatalog.api.EventsSubscriptionApiTestSpec;
import org.fiware.productcatalog.model.EventSubscriptionInputVO;
import org.fiware.productcatalog.model.EventSubscriptionInputVOTestExample;
import org.fiware.productcatalog.model.EventSubscriptionVO;
import org.fiware.productcatalog.model.EventSubscriptionVOTestExample;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(packages = { "org.fiware.tmforum.productcatalog" })
public class EventSubscriptionApiIT implements EventsSubscriptionApiTestSpec {

    private static final String ANY_CALLBACK = "https://test.com";
    private static final String ANY_SIMPLE_QUERY = "name==%22test%22";
    private static final String ANY_COMPLEX_QUERY =
            "eventType=SomeStateChangeNotification&event.someEntity.severity=Urgent";

    public final EventsSubscriptionApiTestClient eventsSubscriptionApiTestClient;
    private EventSubscriptionInputVO eventSubscriptionInputVO;
    private String message;
    private EventSubscriptionVO expectedEventSubscription;


    public EventSubscriptionApiIT(EventsSubscriptionApiTestClient eventsSubscriptionApiTestClient) {
        this.eventsSubscriptionApiTestClient = eventsSubscriptionApiTestClient;
    }

    public <T> HttpResponse<T> callAndCatch(Callable<HttpResponse<T>> request) throws Exception {
        try {
            return request.call();
        } catch (HttpClientResponseException e) {
            return (HttpResponse<T>) e.getResponse();
        }
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
        HttpResponse<EventSubscriptionVO> eventSubscriptionVOHttpResponse =
                callAndCatch(() -> eventsSubscriptionApiTestClient.registerListener(eventSubscriptionInputVO));
        assertEquals(HttpStatus.CREATED, eventSubscriptionVOHttpResponse.getStatus(), message);
        assertTrue(eventSubscriptionVOHttpResponse.getBody().isPresent());
        expectedEventSubscription.setId(eventSubscriptionVOHttpResponse.getBody().get().getId());

        assertEquals(expectedEventSubscription, eventSubscriptionVOHttpResponse.getBody().get(), message);
    }

    @Override
    public void registerListener400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Override
    public void registerListener401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Override
    public void registerListener403() throws Exception {

    }

    @Override
    public void registerListener404() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Override
    public void registerListener405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Override
    public void registerListener409() throws Exception {

    }

    @Override
    public void registerListener500() throws Exception {

    }

    @Override
    public void unregisterListener204() throws Exception {
        EventSubscriptionInputVO eventSubscriptionInputVO = EventSubscriptionInputVOTestExample.build()
                        .query(null)
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

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Override
    public void unregisterListener401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Override
    public void unregisterListener403() throws Exception {

    }

    @Override
    public void unregisterListener404() throws Exception {

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
                Arguments.of("A listener with callback should have been created.",
                        EventSubscriptionInputVOTestExample.build()
                                .query(null)
                                .callback(ANY_CALLBACK),
                        EventSubscriptionVOTestExample.build()
                                .query(null)
                                .callback(ANY_CALLBACK)
                )
        );
        testEntries.add(
                Arguments.of("A listener with callback and simple query should have been created.",
                        EventSubscriptionInputVOTestExample.build()
                                .query(ANY_SIMPLE_QUERY)
                                .callback(ANY_CALLBACK),
                        EventSubscriptionVOTestExample.build()
                                .query(ANY_SIMPLE_QUERY)
                                .callback(ANY_CALLBACK)
                )
        );
        testEntries.add(
                Arguments.of("A listener with callback and complex query should have been created.",
                        EventSubscriptionInputVOTestExample.build()
                                .query(ANY_COMPLEX_QUERY)
                                .callback(ANY_CALLBACK),
                        EventSubscriptionVOTestExample.build()
                                .query(ANY_COMPLEX_QUERY)
                                .callback(ANY_CALLBACK)
                )
        );

        return testEntries.stream();
    }
}
