package org.fiware.tmforum.migration.postgresql;

import io.micronaut.context.ApplicationContext;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.RelationshipListVO;
import org.fiware.ngsi.model.RelationshipVO;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.common.repository.NgsiLdEntityRepository;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.product.CategoryRef;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.resource.ResourceCandidate;
import org.fiware.tmforum.resource.ResourceCandidateRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The Orion-LD replace path of the broker backfill when a delete or the write fails: only what was actually deleted is
 * restored, and nothing is lost silently.
 */
class BrokerReferenceBackfillTest {

	// nothing listens there; every broker call goes through the mocks
	private static final String UNUSED_BROKER_URL = "http://127.0.0.1:1";
	private static final int PAGE_SIZE = 10;
	private static final int NO_FAILING_DELETE = -1;
	private static final String CATEGORY = "category";
	private static final String RESOURCE_CANDIDATE = "resourceCandidate";
	private static final URI OFFERING_ID = URI.create("urn:ngsi-ld:product-offering:offering");
	private static final URI CANDIDATE_ID = URI.create("urn:ngsi-ld:resource-candidate:candidate");
	private static final URI CATEGORY_ID = URI.create("urn:ngsi-ld:category:category");

	private final BrokerSource source = mock(BrokerSource.class);
	private final NgsiLdEntityRepository repository = mock(NgsiLdEntityRepository.class);
	private final List<String> deletes = new ArrayList<>();
	private final List<Object> updates = new ArrayList<>();
	private ApplicationContext context;

	@BeforeEach
	void startSourceContext() {
		MigrationContexts contexts = new MigrationContexts();
		new CommandLine(contexts).parseArgs("--broker-url", UNUSED_BROKER_URL);
		context = ApplicationContext.builder()
				.deduceEnvironment(false)
				.deduceCloudEnvironment(false)
				.environments(contexts.sourceEnvironments().toArray(String[]::new))
				.properties(contexts.sourceProperties())
				.start();
	}

	@AfterEach
	void closeAndResetEscaper() {
		context.close();
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	static Stream<Arguments> failures() {
		return Stream.of(
				Arguments.of("the write fails, the restore succeeds", NO_FAILING_DELETE, false,
						"were restored"),
				Arguments.of("the write fails, the restore fails", NO_FAILING_DELETE, true,
						"restore them by hand"),
				Arguments.of("the second delete fails", 1, false, "were restored"),
				Arguments.of("the first delete fails", 0, false, "its read value was"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("failures")
	void restoresOnlyWhatWasDeleted(String scenario, int failingDelete, boolean restoreFails, String expectedError) {
		seedOfferingWithDatasetIdInstances();
		doAnswer(invocation -> {
			String attribute = invocation.getArgument(1);
			if (deletes.size() == failingDelete) {
				throw new IllegalStateException("delete of " + attribute + " timed out");
			}
			deletes.add(attribute);
			return null;
		}).when(source).deleteAttribute(eq(OFFERING_ID), anyString());
		when(repository.updateDomainEntity(anyString(), any())).thenAnswer(invocation -> {
			updates.add(invocation.getArgument(1));
			boolean restoring = failingDelete != NO_FAILING_DELETE || updates.size() > 1;
			return !restoring || restoreFails
					? Mono.error(new IllegalStateException("broker unavailable"))
					: Mono.empty();
		});

		ReferenceBackfillReport report = new BrokerReferenceBackfill(context, source, repository,
				ReferenceBackfillCatalog.load(), EntityTypeRegistry.scan(), PAGE_SIZE).run(false);

		assertFalse(report.succeeded(), scenario);
		assertEquals(1, report.errors().size(), report.errors().toString());
		assertTrue(report.errors().getFirst().contains(expectedError), report.errors().getFirst());
		if (failingDelete == NO_FAILING_DELETE) {
			assertEquals(Set.of(CATEGORY, RESOURCE_CANDIDATE), Set.copyOf(deletes));
			assertEquals(2, updates.size(), "The write and the restore.");
		} else {
			assertEquals(failingDelete, deletes.size());
			assertEquals(failingDelete == 0 ? 0 : 1, updates.size(),
					"No write after a failed delete, and a restore only if something was deleted.");
		}
		if (!deletes.isEmpty()) {
			assertEquals(Set.copyOf(deletes), writtenAttributes((ProductOffering) updates.getLast()),
					"The restore must carry exactly the deleted attributes.");
		}
	}

	// the mapping library writes a single reference (and a one-element list) as the default instance, so stored
	// instances carrying a datasetId differ and are replaced
	private void seedOfferingWithDatasetIdInstances() {
		RelationshipListVO categories = new RelationshipListVO();
		categories.add(new RelationshipVO()._object(CATEGORY_ID).datasetId(CATEGORY_ID));
		EntityVO offeringVO = entity(OFFERING_ID, ProductOffering.TYPE_PRODUCT_OFFERING);
		offeringVO.setAdditionalProperties(RESOURCE_CANDIDATE,
				new RelationshipVO()._object(CANDIDATE_ID).datasetId(CANDIDATE_ID));
		offeringVO.setAdditionalProperties(CATEGORY, categories);
		ProductOffering offering = new ProductOffering(OFFERING_ID.toString());
		offering.setResourceCandidate(new ResourceCandidateRef(CANDIDATE_ID.toString()));
		offering.setCategory(List.of(new CategoryRef(CATEGORY_ID.toString())));

		EntityVO candidateVO = entity(CANDIDATE_ID, ResourceCandidate.TYPE_RESOURCE_CANDIDATE);
		ResourceCandidate candidate = new ResourceCandidate(CANDIDATE_ID.toString());
		candidate.setName("candidate");
		EntityVO categoryVO = entity(CATEGORY_ID, Category.TYPE_CATEGORY);
		Category category = new Category(CATEGORY_ID.toString());
		category.setName("category");

		when(source.page(anyString(), anyInt(), anyInt())).thenReturn(new EntitySource.Page(List.of(), 0));
		when(source.page(eq(ProductOffering.TYPE_PRODUCT_OFFERING), eq(0), anyInt()))
				.thenReturn(new EntitySource.Page(List.of(offeringVO), 1));
		when(source.entities(any())).thenReturn(List.of(candidateVO, categoryVO));
		when(source.toDomain(offeringVO, ProductOffering.class)).thenReturn(offering);
		when(source.toDomain(candidateVO, ResourceCandidate.class)).thenReturn(candidate);
		when(source.toDomain(categoryVO, Category.class)).thenReturn(category);
	}

	private static EntityVO entity(URI id, String type) {
		return new EntityVO().id(id).type(type);
	}

	private static Set<String> writtenAttributes(ProductOffering update) {
		Set<String> attributes = new HashSet<>();
		if (update.getCategory() != null) {
			attributes.add(CATEGORY);
		}
		if (update.getResourceCandidate() != null) {
			attributes.add(RESOURCE_CANDIDATE);
		}
		return attributes;
	}
}
