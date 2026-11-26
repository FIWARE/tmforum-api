package org.fiware.tmforum.common.notification.checkers.tmforum;

import org.fiware.tmforum.common.querying.SubscriptionQueryResolver;
import org.fiware.tmforum.common.rest.CharacterizationEntity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteTmForumEventCheckerTest {

	private static final String PAYLOAD_NAME = "characterizationEntity";
	private static final String ENTITY_NAME = "deleted";

	@ParameterizedTest(name = "{0}")
	@MethodSource("hubQueries")
	void firesWhenTheDeletedEntityMatchedTheHubQuery(String description, String query, boolean expected) {
		CharacterizationEntity oldState = new CharacterizationEntity("urn:ngsi-ld:characterization-entity:1");
		oldState.setName(ENTITY_NAME);

		DeleteTmForumEventChecker<CharacterizationEntity> checker =
				new DeleteTmForumEventChecker<>(new SubscriptionQueryResolver(), oldState, PAYLOAD_NAME);

		assertEquals(expected, checker.wasFired(query), description);
	}

	private static Stream<Arguments> hubQueries() {
		return Stream.of(
				Arguments.of("no query", null, true),
				Arguments.of("empty query", "", true),
				Arguments.of("matching query", PAYLOAD_NAME + ".name=" + ENTITY_NAME, true),
				Arguments.of("not matching query", PAYLOAD_NAME + ".name=other", false));
	}
}
