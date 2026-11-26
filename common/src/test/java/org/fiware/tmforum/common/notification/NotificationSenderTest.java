package org.fiware.tmforum.common.notification;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.tmforum.common.domain.subscription.Event;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSenderTest {

	private static final String CALLBACK_PREFIX = "http://hub-";

	@ParameterizedTest(name = "{0}")
	@MethodSource("callbackAnswers")
	void deliverNotificationsCompletesOnlyWhenEveryCallbackAcknowledged(String description,
																		List<Mono<HttpResponse<Object>>> answers,
																		Class<? extends Throwable> expectedFailure) {
		HttpClient httpClient = mock(HttpClient.class);
		when(httpClient.exchange(any(HttpRequest.class), eq(Object.class))).thenAnswer(invocation -> {
			HttpRequest<?> request = invocation.getArgument(0);
			return answers.get(Integer.parseInt(request.getUri().getHost().substring("hub-".length())));
		});
		List<TMForumNotification> notifications = Stream.iterate(0, i -> i + 1).limit(answers.size())
				.map(i -> new TMForumNotification(URI.create(CALLBACK_PREFIX + i), new Event()))
				.toList();

		Mono<Void> delivery = new NotificationSender(httpClient).deliverNotifications(notifications);
		if (expectedFailure == null) {
			assertDoesNotThrow(() -> delivery.block());
		} else {
			assertThrows(expectedFailure, delivery::block);
		}
		// one attempt per callback, a failure does not stop the others
		verify(httpClient, times(answers.size())).exchange(any(HttpRequest.class), eq(Object.class));
	}

	private static Stream<Arguments> callbackAnswers() {
		Class<NotificationSender.NotificationDeliveryException> notAcknowledged =
				NotificationSender.NotificationDeliveryException.class;
		// the Micronaut client signals a 4xx/5xx answer as an error, not as a response
		Mono<HttpResponse<Object>> serverError = Mono.error(new HttpClientResponseException(
				HttpStatus.BAD_GATEWAY.getReason(), HttpResponse.status(HttpStatus.BAD_GATEWAY)));
		Mono<HttpResponse<Object>> connectionError = Mono.error(new HttpClientException("Connection refused"));
		return Stream.of(
				Arguments.of("no callbacks", List.of(), null),
				Arguments.of("all 2xx", List.of(answer(HttpStatus.OK), answer(HttpStatus.CREATED),
						answer(HttpStatus.NO_CONTENT)), null),
				Arguments.of("first fails", List.of(answer(HttpStatus.BAD_GATEWAY), answer(HttpStatus.OK),
						answer(HttpStatus.OK)), notAcknowledged),
				Arguments.of("last fails", List.of(answer(HttpStatus.OK), answer(HttpStatus.OK),
						answer(HttpStatus.NOT_FOUND)), notAcknowledged),
				Arguments.of("redirect is no acknowledgement", List.of(answer(HttpStatus.MOVED_PERMANENTLY)),
						notAcknowledged),
				Arguments.of("first signals a 5xx error", List.of(serverError, answer(HttpStatus.OK),
						answer(HttpStatus.OK)), Throwable.class),
				Arguments.of("last is unreachable", List.of(answer(HttpStatus.OK), answer(HttpStatus.OK),
						connectionError), Throwable.class),
				Arguments.of("several fail", List.of(connectionError, answer(HttpStatus.OK), serverError),
						Throwable.class));
	}

	private static Mono<HttpResponse<Object>> answer(HttpStatus status) {
		return Mono.just(HttpResponse.status(status));
	}
}
