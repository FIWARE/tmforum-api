package org.fiware.tmforum.common.rest;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.fiware.ngsi.model.NotUpdatedDetailsVO;
import org.fiware.ngsi.model.UpdateResultVO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * {@link ErrorResponseCharacterizationTest} with {@code replaceOnUpdate}, plus the failures of its pre-read and its
 * partial (207) write.
 */
class ReplaceOnUpdateErrorResponseCharacterizationTest extends ErrorResponseCharacterizationTest {

	private static final String PATCH_PATH = CharacterizationTestController.BASE_PATH
			+ CharacterizationTestController.ENTITIES_PATH;
	private static final String DIRECT_PATH = CharacterizationTestController.BASE_PATH
			+ CharacterizationTestController.DIRECT_PATH;
	private static final String NOT_UPDATED_ATTRIBUTE = "name";
	private static final String NOT_UPDATED_REASON = "invalid value";

	@Override
	protected boolean replaceOnUpdate() {
		return true;
	}

	@Override
	protected void scriptUpdateFails(Failure failure) {
		broker.answer("retrieveEntityAsJson", args -> Mono.just(HttpResponse.ok(Map.of())));
		broker.answer("updateEntityAttributes", args -> failure.mono());
	}

	@Override
	protected void scriptRelatedUpdateFails() {
		broker.answer("retrieveEntityAsJson", args -> Failure.NOT_FOUND.mono());
	}

	static Stream<Arguments> failedPreReads() {
		return Stream.of(PATCH_PATH, DIRECT_PATH).flatMap(path -> Stream.of(
				Arguments.of(path, Failure.NOT_FOUND, caInternal()),
				Arguments.of(path, Failure.INTERNAL_SERVER_ERROR, caInternal()),
				Arguments.of(path, Failure.NO_ANSWER, badGateway())));
	}

	@ParameterizedTest(name = "{0} {1} -> {2}")
	@MethodSource("failedPreReads")
	void updateFailsOnThePreRead(String path, Failure failure, Expected expected) {
		String id = newEntityId();
		if (PATCH_PATH.equals(path)) {
			scriptStored(id);
		}
		broker.answer("retrieveEntityAsJson", args -> failure.mono());

		assertResponse(expected, HttpRequest.PATCH(path + "/" + id, ""));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {PATCH_PATH, DIRECT_PATH})
	void updateFailsOnAPartialWrite(String path) {
		String id = newEntityId();
		if (PATCH_PATH.equals(path)) {
			scriptStored(id);
		}
		broker.answer("retrieveEntityAsJson", args -> Mono.just(HttpResponse.ok(Map.of())));
		broker.answer("updateEntityAttributes", args -> Mono.just(HttpResponse.status(HttpStatus.MULTI_STATUS)
				.body((Object) new UpdateResultVO().notUpdated(List.of(new NotUpdatedDetailsVO()
						.attributeName(NOT_UPDATED_ATTRIBUTE).reason(NOT_UPDATED_REASON))))));

		assertResponse(new Expected(HttpStatus.BAD_REQUEST, String.format(
						"The request contained invalid data - Not all attributes could be updated: %s: %s",
						NOT_UPDATED_ATTRIBUTE, NOT_UPDATED_REASON)),
				HttpRequest.PATCH(path + "/" + id, ""));
	}
}
