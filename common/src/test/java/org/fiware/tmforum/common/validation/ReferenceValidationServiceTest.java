package org.fiware.tmforum.common.validation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.ReferenceProperties;
import org.fiware.tmforum.common.exception.NonExistentReferenceException;
import org.fiware.tmforum.common.repository.ReferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferenceValidationServiceTest {

	private static final String CATEGORY_TYPE = "category";
	private static final String OFFERING_TYPE = "product-offering";

	private static final TestRef CATEGORY_A = categoryRef("urn:ngsi-ld:category:a");
	private static final TestRef CATEGORY_B = categoryRef("urn:ngsi-ld:category:b");
	private static final TestRef CATEGORY_C = categoryRef("urn:ngsi-ld:category:c");
	private static final TestRef MISSING_X = categoryRef("urn:ngsi-ld:category:missing-x");
	private static final TestRef MISSING_Y = categoryRef("urn:ngsi-ld:category:missing-y");
	// a category reference pointing at an existing entity of another type
	private static final TestRef WRONG_TYPE = categoryRef("urn:ngsi-ld:product-offering:o");

	private static final String EXTERNAL_HOST = "partner-a.example.org";
	private static final URI EXTERNAL_HREF = URI.create("https://" + EXTERNAL_HOST + "/category/1");
	// external by their href; the id decides what the local type guard finds
	private static final TestRef EXTERNAL_1 = externalRef(URI.create("urn:ngsi-ld:category:remote-1"));
	private static final TestRef EXTERNAL_2 = externalRef(URI.create("urn:ngsi-ld:category:remote-2"));
	private static final TestRef EXTERNAL_ACCEPTED = externalRef(CATEGORY_A.id());
	private static final TestRef EXTERNAL_WRONG_TYPE = externalRef(WRONG_TYPE.id());

	// external systems may use plain ids; the broker only accepts URIs, so such a reference cannot resolve
	private static final TestRef NON_URI_ID = categoryRef("1234");
	private static final TestRef EXTERNAL_NON_URI_ID = externalRef(URI.create("1234"));
	private static final TestRef NULL_ID = new TestRef(null, List.of(CATEGORY_TYPE), null);

	/** Entities present in local storage, id to type. */
	private static final Map<String, String> STORED_ENTITIES = Map.of(
			CATEGORY_A.id().toString(), CATEGORY_TYPE,
			CATEGORY_B.id().toString(), CATEGORY_TYPE,
			CATEGORY_C.id().toString(), CATEGORY_TYPE,
			WRONG_TYPE.id().toString(), OFFERING_TYPE);

	private ReferencesRepository referencesRepository;
	private SimpleMeterRegistry meterRegistry;
	private ReferenceClassifier classifier;
	private ReferenceValidationService service;

	@BeforeEach
	public void setUp() {
		referencesRepository = mock(ReferencesRepository.class);
		when(referencesRepository.referenceExists(anyString(), anyList())).thenAnswer(invocation -> {
			String type = STORED_ENTITIES.get(invocation.<String>getArgument(0));
			List<String> acceptedTypes = invocation.getArgument(1);
			return type != null && acceptedTypes.contains(type) ? Mono.just(new EntityVO()) : Mono.empty();
		});
		when(referencesRepository.findReference(anyString())).thenAnswer(invocation -> {
			String type = STORED_ENTITIES.get(invocation.<String>getArgument(0));
			return type == null ? Mono.empty() : Mono.just(new EntityVO().type(type));
		});
		meterRegistry = new SimpleMeterRegistry();
		ReferenceProperties referenceProperties = new ReferenceProperties();
		referenceProperties.setExternalHosts(List.of(EXTERNAL_HOST));
		classifier = new ReferenceClassifier(referenceProperties, new GeneralProperties());
		service = new ReferenceValidationService(referencesRepository, classifier, meterRegistry);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("referenceCombinations")
	public void checkReferenceExistsRequiresAllReferencesToResolve(String description, List<TestRef> refs,
			boolean expected) {
		assertEquals(expected, service.checkReferenceExists(refs).block());
	}

	private static Stream<Arguments> referenceCombinations() {
		return Stream.of(
				Arguments.of("null list", null, true),
				Arguments.of("empty list", List.of(), true),
				Arguments.of("only nulls", Arrays.asList(null, null), true),
				Arguments.of("single reference resolves", List.of(CATEGORY_A), true),
				Arguments.of("all resolve", List.of(CATEGORY_A, CATEGORY_B, CATEGORY_C), true),
				Arguments.of("resolving references mixed with nulls", Arrays.asList(CATEGORY_A, null), true),
				Arguments.of("one of three missing", List.of(CATEGORY_A, MISSING_X, CATEGORY_B), false),
				Arguments.of("wrong type", List.of(WRONG_TYPE), false),
				Arguments.of("all missing", List.of(MISSING_X, MISSING_Y), false),
				Arguments.of("id that is not a URI", List.of(NON_URI_ID), false),
				Arguments.of("external reference with an id that is not a URI", List.of(EXTERNAL_NON_URI_ID), false),
				Arguments.of("reference without an id", List.of(NULL_ID), false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("externalCombinations")
	public void externalReferencesAreSkippedAndCounted(String description, List<TestRef> refs, boolean expected,
			double localResolved, double localUnresolved, double externalResolved, double externalUnresolved,
			double externalSkipped) {
		assertEquals(expected, service.checkReferenceExists(refs).block());

		assertEquals(localResolved, count(ReferenceValidationService.SCOPE_LOCAL,
				ReferenceValidationService.RESULT_RESOLVED));
		assertEquals(localUnresolved, count(ReferenceValidationService.SCOPE_LOCAL,
				ReferenceValidationService.RESULT_UNRESOLVED));
		assertEquals(externalResolved, count(ReferenceValidationService.SCOPE_EXTERNAL,
				ReferenceValidationService.RESULT_RESOLVED));
		assertEquals(externalUnresolved, count(ReferenceValidationService.SCOPE_EXTERNAL,
				ReferenceValidationService.RESULT_UNRESOLVED));
		assertEquals(externalSkipped, count(ReferenceValidationService.SCOPE_EXTERNAL,
				ReferenceValidationService.RESULT_SKIPPED));
	}

	private static Stream<Arguments> externalCombinations() {
		return Stream.of(
				Arguments.of("local existing and external", List.of(CATEGORY_A, EXTERNAL_1), true, 1, 0, 0, 0, 1),
				Arguments.of("local missing and external", List.of(MISSING_X, EXTERNAL_1), false, 0, 1, 0, 0, 1),
				Arguments.of("all external", List.of(EXTERNAL_1, EXTERNAL_2), true, 0, 0, 0, 0, 2),
				Arguments.of("external with local entity of accepted type", List.of(EXTERNAL_ACCEPTED), true, 0, 0,
						1, 0, 0),
				Arguments.of("external with local entity of another type", List.of(EXTERNAL_WRONG_TYPE), false, 0,
						0, 0, 1, 0));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("nonUriIds")
	public void referencesWithAnIdThatIsNotAUriAreNotLookedUp(String description, TestRef ref, String scope) {
		assertEquals(false, service.checkReferenceExists(List.of(ref)).block());

		verify(referencesRepository, never()).referenceExists(anyString(), anyList());
		verify(referencesRepository, never()).findReference(anyString());
		assertEquals(1, count(scope, ReferenceValidationService.RESULT_UNRESOLVED));
	}

	private static Stream<Arguments> nonUriIds() {
		return Stream.of(
				Arguments.of("local reference", NON_URI_ID, ReferenceValidationService.SCOPE_LOCAL),
				Arguments.of("external reference", EXTERNAL_NON_URI_ID, ReferenceValidationService.SCOPE_EXTERNAL),
				Arguments.of("reference without an id", NULL_ID, ReferenceValidationService.SCOPE_LOCAL));
	}

	@Test
	public void externalReferencesAreOnlyLookedUpLocallyById() {
		assertEquals(true, service.checkReferenceExists(List.of(CATEGORY_A, EXTERNAL_1)).block());

		verify(referencesRepository).referenceExists(eq(CATEGORY_A.id().toString()), anyList());
		verify(referencesRepository, never()).referenceExists(eq(EXTERNAL_1.id().toString()), anyList());
		verify(referencesRepository).findReference(EXTERNAL_1.id().toString());
		verify(referencesRepository, never()).findReference(CATEGORY_A.id().toString());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("externalCombinations")
	public void validatesWithoutMeterRegistry(String description, List<TestRef> refs, boolean expected,
			double localResolved, double localUnresolved, double externalResolved, double externalUnresolved,
			double externalSkipped) {
		ReferenceValidationService unmetered = new ReferenceValidationService(referencesRepository, classifier, null);

		assertEquals(expected, unmetered.checkReferenceExists(refs).block());
	}

	@Test
	public void getCheckingMonoNamesTheWrongTypedExternalReference() {
		Exception exception = assertThrows(Exception.class, () -> service
				.getCheckingMono(List.of(EXTERNAL_1, EXTERNAL_WRONG_TYPE), new Object()).block());

		assertEquals(NonExistentReferenceException.class, exception.getCause().getClass());
		assertEquals(String.format("References do not exist: [%s]", WRONG_TYPE.id()),
				exception.getCause().getMessage());
	}

	@Test
	public void findUnresolvedCanBeSubscribedRepeatedly() {
		Mono<List<TestRef>> unresolved = service.findUnresolved(List.of(CATEGORY_A, MISSING_X));

		assertEquals(List.of(MISSING_X), unresolved.block());
		assertEquals(List.of(MISSING_X), unresolved.block());
	}

	@Test
	public void getCheckingMonoReturnsTheObjectIfAllReferencesResolve() {
		Object returnObject = new Object();

		assertSame(returnObject, service.getCheckingMono(List.of(CATEGORY_A, CATEGORY_B), returnObject).block());
	}

	@Test
	public void getCheckingMonoNamesExactlyTheUnresolvedIdsInInputOrder() {
		List<TestRef> refs = List.of(MISSING_Y, CATEGORY_A, WRONG_TYPE, CATEGORY_B, MISSING_X);

		Exception exception = assertThrows(Exception.class, () -> service.getCheckingMono(refs, new Object()).block());

		assertEquals(NonExistentReferenceException.class, exception.getCause().getClass());
		assertEquals(String.format("References do not exist: [%s, %s, %s]", MISSING_Y.id(), WRONG_TYPE.id(),
				MISSING_X.id()), exception.getCause().getMessage());
	}

	private double count(String scope, String result) {
		Counter counter = meterRegistry.find(ReferenceValidationService.REFERENCES_METRIC)
				.tag(ReferenceValidationService.SCOPE_TAG, scope)
				.tag(ReferenceValidationService.RESULT_TAG, result)
				.counter();
		return counter == null ? 0 : counter.count();
	}

	private static TestRef categoryRef(String id) {
		return new TestRef(URI.create(id), List.of(CATEGORY_TYPE), null);
	}

	private static TestRef externalRef(URI id) {
		return new TestRef(id, List.of(CATEGORY_TYPE), EXTERNAL_HREF);
	}

	private record TestRef(URI id, List<String> referencedTypes, URI href) implements ReferencedEntity {

		@Override
		public URI getReferenceHref() {
			return href;
		}

		@Override
		public List<String> getReferencedTypes() {
			return referencedTypes;
		}

		@Override
		public URI getEntityId() {
			return id;
		}
	}
}
