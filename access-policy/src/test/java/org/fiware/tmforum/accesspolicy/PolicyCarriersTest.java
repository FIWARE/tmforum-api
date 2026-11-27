package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.ConfigurationException;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the governed-type lookup of {@link PolicyCarriers}: the ungoverned short-circuit, subclasses, and the
 * ambiguities it refuses at startup or resolves to an unreadable result.
 */
class PolicyCarriersTest {

	private static final String UID = "urn:policy:offering:access";
	private static final Map<String, Object> CATALOG_POLICY = Map.of(
			"@context", "http://www.w3.org/ns/odrl.jsonld",
			"odrl:uid", UID,
			"permission", List.of(Map.of("action", "read", "dspace:scope", "catalog")));

	static class Governed {
	}

	static class GovernedChild extends Governed {
	}

	static class Ungoverned {
	}

	interface Visible {
	}

	interface Orderable {
	}

	static class VisibleAndOrderable implements Visible, Orderable {
	}

	/** Carries {@link #CATALOG_POLICY} on every instance of its type. */
	record FixedCarrier<T>(Class<T> carriedType) implements PolicyCarrier<T> {

		@Override
		public List<CarriedPolicy> carriedPolicies(T entity) {
			return List.of(new CarriedPolicy("accessPolicy", CATALOG_POLICY));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void anUngovernedEntityIsNeverRead() {
		PolicyCarrier<Governed> carrier = mock(PolicyCarrier.class);
		when(carrier.carriedType()).thenReturn(Governed.class);
		Ungoverned entity = mock(Ungoverned.class);
		PolicyCarriers carriers = new PolicyCarriers(List.of(carrier));

		assertSame(ResolvedPolicies.NONE, carriers.resolve(entity, PolicyScope.CATALOG));
		assertFalse(carriers.isGoverned(Ungoverned.class));
		verifyNoInteractions(entity);
		verify(carrier, never()).carriedPolicies(any());
		verify(carrier, never()).resolve(any(), any());
	}

	@Test
	void aNullEntityResolvesToNone() {
		PolicyCarriers carriers = new PolicyCarriers(List.of(new FixedCarrier<>(Governed.class)));

		assertSame(ResolvedPolicies.NONE, carriers.resolve(null, PolicyScope.CATALOG));
	}

	@ParameterizedTest
	@EnumSource(PolicyScope.class)
	void aSubclassResolvesThroughItsParentsCarrier(PolicyScope scope) {
		PolicyCarriers carriers = new PolicyCarriers(List.of(new FixedCarrier<>(Governed.class)));

		assertTrue(carriers.isGoverned(GovernedChild.class));
		ResolvedPolicies resolved = carriers.resolve(new GovernedChild(), scope);
		assertEquals(scope == PolicyScope.CATALOG ? Set.of(UID) : Set.of(), resolved.policies().keySet());
		assertEquals(List.of(), resolved.unreadable());
	}

	static Stream<Named<List<PolicyCarrier<?>>>> relatedCarriers() {
		return Stream.of(
				Named.of("one type twice", List.of(new FixedCarrier<>(Governed.class),
						new FixedCarrier<>(Governed.class))),
				Named.of("a type and its subclass", List.of(new FixedCarrier<>(GovernedChild.class),
						new FixedCarrier<>(Governed.class))),
				Named.of("an interface and its implementation", List.of(new FixedCarrier<>(Visible.class),
						new FixedCarrier<>(VisibleAndOrderable.class))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("relatedCarriers")
	void relatedCarriersFailConstruction(List<PolicyCarrier<?>> carriers) {
		assertThrows(ConfigurationException.class, () -> new PolicyCarriers(carriers));
	}

	@Test
	void aClassMatchingTwoUnrelatedCarriersIsUnreadable() {
		PolicyCarriers carriers = new PolicyCarriers(List.of(new FixedCarrier<>(Visible.class),
				new FixedCarrier<>(Orderable.class)));

		assertTrue(carriers.isGoverned(VisibleAndOrderable.class));
		ResolvedPolicies resolved = carriers.resolve(new VisibleAndOrderable(), PolicyScope.CATALOG);
		assertEquals(Map.of(), resolved.policies());
		assertEquals(1, resolved.unreadable().size());
		assertEquals(VisibleAndOrderable.class.getName(), resolved.unreadable().getFirst().location());
	}

	@Test
	void aThrowingCarrierResolvesUnreadable() {
		PolicyCarrier<Governed> throwing = new PolicyCarrier<>() {
			@Override
			public Class<Governed> carriedType() {
				return Governed.class;
			}

			@Override
			public List<CarriedPolicy> carriedPolicies(Governed entity) {
				throw new IllegalStateException("broken walk");
			}
		};
		PolicyCarriers carriers = new PolicyCarriers(List.of(throwing));

		ResolvedPolicies resolved = carriers.resolve(new Governed(), PolicyScope.CATALOG);

		assertEquals(Map.of(), resolved.policies());
		assertEquals(1, resolved.unreadable().size());
	}

	@Test
	void anEnabledContextWithoutCarriersGovernsNothing() {
		Map<String, Object> configuration = new HashMap<>(TestCaches.DECISIONS);
		configuration.put(AccessPolicyProperties.ENABLED_PROPERTY, "true");
		configuration.put(AccessPolicyProperties.PREFIX + ".jwt.jwks-uri",
				"https://keycloak.example.org/realms/test/protocol/openid-connect/certs");
		configuration.put(AccessPolicyProperties.PREFIX + ".jwt.issuer", TestTokens.ISSUER);
		configuration.put(AccessPolicyProperties.PREFIX + ".pdp.url", "http://localhost:8080");
		try (ApplicationContext context = ApplicationContext.run(configuration)) {
			assertFalse(context.getBean(PolicyCarriers.class).isGoverned(Object.class));
		}
	}
}
