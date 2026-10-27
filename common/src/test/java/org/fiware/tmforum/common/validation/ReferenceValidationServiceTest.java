package org.fiware.tmforum.common.validation;

import org.fiware.ngsi.model.EntityVO;
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
import static org.mockito.Mockito.mock;
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

	/** Entities present in local storage, id to type. */
	private static final Map<String, String> STORED_ENTITIES = Map.of(
			CATEGORY_A.id().toString(), CATEGORY_TYPE,
			CATEGORY_B.id().toString(), CATEGORY_TYPE,
			CATEGORY_C.id().toString(), CATEGORY_TYPE,
			WRONG_TYPE.id().toString(), OFFERING_TYPE);

	private ReferenceValidationService service;

	@BeforeEach
	public void setUp() {
		ReferencesRepository referencesRepository = mock(ReferencesRepository.class);
		when(referencesRepository.referenceExists(anyString(), anyList())).thenAnswer(invocation -> {
			String type = STORED_ENTITIES.get(invocation.<String>getArgument(0));
			List<String> acceptedTypes = invocation.getArgument(1);
			return type != null && acceptedTypes.contains(type) ? Mono.just(new EntityVO()) : Mono.empty();
		});
		service = new ReferenceValidationService(referencesRepository);
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
				Arguments.of("all missing", List.of(MISSING_X, MISSING_Y), false));
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

	private static TestRef categoryRef(String id) {
		return new TestRef(URI.create(id), List.of(CATEGORY_TYPE));
	}

	private record TestRef(URI id, List<String> referencedTypes) implements ReferencedEntity {

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
