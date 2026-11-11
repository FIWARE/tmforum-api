package org.fiware.tmforum.productcatalog.storage;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.ProductOfferingApiTestClient;
import org.fiware.productcatalog.model.CategoryRefVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.common.test.PostgresqlOnly;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins that a GET of an entity with references costs PostgreSQL exactly one statement: references are stored inline
 * and never resolved on read.
 */
@PostgresqlOnly
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, environments = PostgresStorage.ENVIRONMENT,
		contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = StatementRecorder.ENABLED_PROPERTY, value = "true")
public class GetIssuesOneQueryIT extends AbstractApiIT {

	private static final String OFFERING_ID_PREFIX = "urn:ngsi-ld:" + ProductOffering.TYPE_PRODUCT_OFFERING + ":";
	private static final int ONE_STATEMENT = 1;

	private final ProductOfferingApiTestClient productOfferingApiTestClient;
	private final EntityRepository entityRepository;
	private final StatementRecorder statementRecorder;

	public GetIssuesOneQueryIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
			GeneralProperties generalProperties, ProductOfferingApiTestClient productOfferingApiTestClient,
			EntityRepository entityRepository, StatementRecorder statementRecorder) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productOfferingApiTestClient = productOfferingApiTestClient;
		this.entityRepository = entityRepository;
		this.statementRecorder = statementRecorder;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
		return eventHandler;
	}

	@Test
	public void getWithReferencesIssuesOneStatement() throws Exception {
		Category category = new Category(UUID.randomUUID().toString());
		entityRepository.createDomainEntity(category).block();
		try {
			assertOneStatementPerGet(category);
		} finally {
			// cleanUp only removes offerings
			entityRepository.deleteDomainEntity(category.getId()).block();
		}
	}

	private void assertOneStatementPerGet(Category category) throws Exception {
		ProductOfferingCreateVO createVO = ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
				.productSpecification(null)
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null)
				.category(List.of(CategoryRefVOTestExample.build().atSchemaLocation(null)
						.id(category.getId().toString())
						.href(category.getId())));
		HttpResponse<ProductOfferingVO> created = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, createVO));
		assertEquals(HttpStatus.CREATED, created.getStatus(), "The offering should have been created.");
		String id = created.body().getId();
		assertTrue(id.startsWith(OFFERING_ID_PREFIX) && id.length() > OFFERING_ID_PREFIX.length(),
				"The id should keep the urn:ngsi-ld:product-offering:<id> format, was " + id);

		statementRecorder.reset();
		HttpResponse<ProductOfferingVO> retrieved = callAndCatch(
				() -> productOfferingApiTestClient.retrieveProductOffering(null, id, null));

		assertEquals(HttpStatus.OK, retrieved.getStatus(), "The offering should be retrievable.");
		assertEquals(id, retrieved.body().getId(), "The retrieved id should keep the urn:ngsi-ld format.");
		assertEquals(category.getId().toString(), retrieved.body().getCategory().get(0).getId(),
				"The reference should be returned.");
		List<String> statements = statementRecorder.statements();
		assertEquals(ONE_STATEMENT, statements.size(), "The GET should issue exactly one statement: " + statements);
	}

	@Override
	protected String getEntityType() {
		return ProductOffering.TYPE_PRODUCT_OFFERING;
	}
}
