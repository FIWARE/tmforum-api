package org.fiware.tmforum.common.notification;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
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
	void deliverNotificationsCompletesOnlyWhenEveryCallbackAcknowledged(String description, List<HttpStatus> answers,
																		boolean expectSuccess) {
		HttpClient httpClient = mock(HttpClient.class);
		when(httpClient.exchange(any(HttpRequest.class), eq(Object.class))).thenAnswer(invocation -> {
			HttpRequest<?> request = invocation.getArgument(0);
			int hub = Integer.parseInt(request.getUri().getHost().substring("hub-".length()));
			return Mono.just(HttpResponse.status(answers.get(hub)));
		});
		List<TMForumNotification> notifications = Stream.iterate(0, i -> i + 1).limit(answers.size())
				.map(i -> new TMForumNotification(URI.create(CALLBACK_PREFIX + i), new Event()))
				.toList();

		Mono<Void> delivery = new NotificationSender(httpClient).deliverNotifications(notifications);
		if (expectSuccess) {
			assertDoesNotThrow(() -> delivery.block());
		} else {
			assertThrows(NotificationSender.NotificationDeliveryException.class, delivery::block);
		}
		// one attempt per callback, a failure does not stop the others
		verify(httpClient, times(answers.size())).exchange(any(HttpRequest.class), eq(Object.class));
	}

	private static Stream<Arguments> callbackAnswers() {
		return Stream.of(
				Arguments.of("no callbacks", List.of(), true),
				Arguments.of("all 2xx", List.of(HttpStatus.OK, HttpStatus.CREATED, HttpStatus.NO_CONTENT), true),
				Arguments.of("first fails", List.of(HttpStatus.BAD_GATEWAY, HttpStatus.OK, HttpStatus.OK), false),
				Arguments.of("last fails", List.of(HttpStatus.OK, HttpStatus.OK, HttpStatus.NOT_FOUND), false),
				Arguments.of("redirect is no acknowledgement", List.of(HttpStatus.MOVED_PERMANENTLY), false));
	}
}
