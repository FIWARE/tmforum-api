package org.fiware.tmforum.accesspolicy;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that {@link PolicyCarrier#resolve} never throws, whatever the carrier's walk does.
 */
class PolicyCarrierTest {

	/** A domain type stand-in. */
	static final class Offering {
	}

	/** A carrier whose walk is given per test. */
	record TestCarrier(Function<Offering, List<CarriedPolicy>> walk) implements PolicyCarrier<Offering> {

		@Override
		public Class<Offering> carriedType() {
			return Offering.class;
		}

		@Override
		public List<CarriedPolicy> carriedPolicies(Offering entity) {
			return walk.apply(entity);
		}
	}

	static Stream<Named<TestCarrier>> brokenCarriers() {
		return Stream.of(
				Named.of("throws", new TestCarrier(offering -> {
					throw new IllegalStateException("lazy field not loaded");
				})),
				Named.of("returns null", new TestCarrier(offering -> null)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("brokenCarriers")
	void aBrokenWalkIsOneUnreadablePolicy(TestCarrier carrier) {
		for (PolicyScope scope : PolicyScope.values()) {
			ResolvedPolicies resolved = carrier.resolve(new Offering(), scope);

			assertEquals(Map.of(), resolved.policies());
			assertEquals(1, resolved.unreadable().size());
			UnreadablePolicy unreadable = resolved.unreadable().getFirst();
			assertEquals(Offering.class.getName(), unreadable.location());
			assertNull(unreadable.policyUid());
		}
	}
}
