package org.fiware.tmforum.common.notification;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micronaut.context.annotation.Bean;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@Bean
@Slf4j
public class NotificationSender {
    private final HttpClient httpClient;

    private static final RetryRegistry RETRY_REGISTRY = RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(10)
            .intervalFunction(IntervalFunction.ofExponentialBackoff())
            .failAfterMaxAttempts(true)
            .build());

    private Mono<HttpResponse<Object>> sendToClient(TMForumNotification notification) {
        HttpRequest<?> req = HttpRequest.POST(notification.callback(), notification.event())
                .header(HttpHeaders.CONTENT_TYPE, "application/json");
        return Mono.fromDirect(this.httpClient.exchange(req, Object.class));
    }

    public Mono<Void> sendNotifications(List<TMForumNotification> notifications) {
        notifications
                .forEach(notification -> {
                    Retry retry = RETRY_REGISTRY
                            .retry(String.valueOf(notification.hashCode()));
                    sendToClient(notification)
                            .transformDeferred(RetryOperator.of(retry))
                            .doOnError(e -> log.warn("Was not able to deliver notification {}.", notification, e))
                            .subscribe();
                });
        return Mono.empty();
    }
}
