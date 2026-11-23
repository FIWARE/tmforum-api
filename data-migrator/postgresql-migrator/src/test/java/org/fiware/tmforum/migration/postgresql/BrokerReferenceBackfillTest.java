package org.fiware.tmforum.migration.postgresql;

import io.micronaut.context.ApplicationContext;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.RelationshipListVO;
import org.fiware.ngsi.model.RelationshipVO;
import org.fiware.tmforum.account.domain.AccountBalance;
import org.fiware.tmforum.account.domain.BillingAccount;
import org.fiware.tmforum.account.domain.FinancialAccount;
import org.fiware.tmforum.account.domain.FinancialAccountRef;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.common.repository.NgsiLdEntityRepository;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.product.CategoryRef;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.resource.ResourceCandidate;
import org.fiware.tmforum.resource.ResourceCandidateRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The write paths of the broker backfill: on Orion-LD a failed delete or write restores only what was actually
 * deleted, Scorpio writes without deleting in either replaceOnUpdate mode, and a target whose fillable keys include
 * a list is read again by id, since the entity query leaves such attributes out.
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
	private static final URI BILLING_ACCOUNT_ID = URI.create("urn:ngsi-ld:billing-account:billing");
	private static final URI FINANCIAL_ACCOUNT_ID = URI.create("urn:ngsi-ld:financialAccount:financial");
	private static final String FINANCIAL_ACCOUNT_NAME = "financial account";
	private static final String BALANCE_TYPE = "deposit";

	private final BrokerSource source = mock(BrokerSource.class);
	private final NgsiLdEntityRepository repository = mock(NgsiLdEntityRepository.class);
	private final List<String> deletes = new ArrayList<>();
	private final List<Object> updates = new ArrayList<>();
	private ApplicationContext context;

	@BeforeEach
	void startSourceContext() {
		context = sourceContext(BrokerType.ORION_LD);
	}

	private static ApplicationContext sourceContext(BrokerType brokerType) {
		return sourceContext(brokerType, false);
	}

	private static ApplicationContext sourceContext(BrokerType brokerType, boolean replaceOnUpdate) {
		MigrationContexts contexts = new MigrationContexts();
		new CommandLine(contexts).parseArgs("--broker-url", UNUSED_BROKER_URL,
				"--broker-type=" + brokerType.getEnvironment(), "--replace-on-update=" + replaceOnUpdate);
		return ApplicationContext.builder()
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

	static Stream<Arguments> scorpioWrites() {
		return Stream.of(
				// below 6.0.0 an append replaces every instance of the attribute (and deleteAll fails)
				Arguments.of(false, false),
				Arguments.of(false, true),
				// from 6.0.0 on, updateDomainEntity's replace path removes the other datasetIds in the same PATCH
				Arguments.of(true, false),
				Arguments.of(true, true));
	}

	// the stored datasetIds differ from the written ones, yet neither Scorpio mode deletes anything first
	@ParameterizedTest(name = "replaceOnUpdate={0}, write fails={1}")
	@MethodSource("scorpioWrites")
	void writesChangedInstancesOnScorpioWithoutDeleting(boolean replaceOnUpdate, boolean writeFails) {
		context.close();
		context = sourceContext(BrokerType.SCORPIO, replaceOnUpdate);
		seedOfferingWithDatasetIdInstances();
		when(repository.updateDomainEntity(anyString(), any())).thenAnswer(invocation -> {
			updates.add(invocation.getArgument(1));
			return writeFails ? Mono.error(new IllegalStateException("broker unavailable")) : Mono.empty();
		});

		ReferenceBackfillReport report = new BrokerReferenceBackfill(context, source, repository,
				ReferenceBackfillCatalog.load(), EntityTypeRegistry.scan(), PAGE_SIZE).run(false);

		assertEquals(!writeFails, report.succeeded(), report.errors().toString());
		verify(source, never()).deleteAttribute(any(), anyString());
		assertEquals(1, updates.size(), "One write and no restore.");
		assertEquals(Set.of(CATEGORY, RESOURCE_CANDIDATE), writtenAttributes((ProductOffering) updates.getFirst()));
		assertEquals(replaceOnUpdate, context.getBean(GeneralProperties.class).isReplaceOnUpdate());
	}

	@Test
	void readsATargetWithAListFillableKeyAgainById() {
		EntityVO billingVO = entity(BILLING_ACCOUNT_ID, BillingAccount.TYPE_BILLINGAC);
		billingVO.setAdditionalProperties("financialAccount", new RelationshipVO()._object(FINANCIAL_ACCOUNT_ID));
		BillingAccount billing = new BillingAccount(BILLING_ACCOUNT_ID.toString());
		billing.setFinancialAccount(new FinancialAccountRef(FINANCIAL_ACCOUNT_ID.toString()));
		// as Orion-LD's query lists it: the multi-instance accountBalance is missing
		EntityVO listedVO = entity(FINANCIAL_ACCOUNT_ID, FinancialAccount.TYPE_FINANCIALAC);
		EntityVO completeVO = entity(FINANCIAL_ACCOUNT_ID, FinancialAccount.TYPE_FINANCIALAC);
		FinancialAccount complete = new FinancialAccount(FINANCIAL_ACCOUNT_ID.toString());
		complete.setName(FINANCIAL_ACCOUNT_NAME);
		AccountBalance balance = new AccountBalance();
		balance.setBalanceType(BALANCE_TYPE);
		complete.setAccountBalance(List.of(balance));

		when(source.page(anyString(), anyInt(), anyInt())).thenReturn(new EntitySource.Page(List.of(), 0));
		when(source.page(eq(BillingAccount.TYPE_BILLINGAC), eq(0), anyInt()))
				.thenReturn(new EntitySource.Page(List.of(billingVO), 1));
		when(source.entities(any())).thenReturn(List.of(listedVO));
		when(source.entity(FINANCIAL_ACCOUNT_ID)).thenReturn(Optional.of(completeVO));
		when(source.toDomain(billingVO, BillingAccount.class)).thenReturn(billing);
		when(source.toDomain(completeVO, FinancialAccount.class)).thenReturn(complete);
		when(repository.updateDomainEntity(anyString(), any())).thenAnswer(invocation -> {
			updates.add(invocation.getArgument(1));
			return Mono.empty();
		});

		ReferenceBackfillReport report = new BrokerReferenceBackfill(context, source, repository,
				ReferenceBackfillCatalog.load(), EntityTypeRegistry.scan(), PAGE_SIZE).run(false);

		assertTrue(report.succeeded(), report.errors().toString());
		verify(source).entity(FINANCIAL_ACCOUNT_ID);
		FinancialAccountRef written = ((BillingAccount) updates.getLast()).getFinancialAccount();
		assertEquals(FINANCIAL_ACCOUNT_NAME, written.getName());
		assertEquals(BALANCE_TYPE, written.getAccountBalance().getBalanceType());
	}

	@Test
	void keepsTheListedTargetWithoutAListFillableKey() {
		seedOfferingWithDatasetIdInstances();

		ReferenceBackfillReport report = new BrokerReferenceBackfill(context, source, repository,
				ReferenceBackfillCatalog.load(), EntityTypeRegistry.scan(), PAGE_SIZE).run(true);

		assertTrue(report.succeeded(), report.errors().toString());
		verify(source, never()).entity(any());
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
