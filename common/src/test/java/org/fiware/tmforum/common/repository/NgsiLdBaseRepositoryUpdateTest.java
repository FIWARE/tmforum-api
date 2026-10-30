package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.NgsiLdWriteOperation;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityFragmentVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.NotUpdatedDetailsVO;
import org.fiware.ngsi.model.PropertyListVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipListVO;
import org.fiware.ngsi.model.RelationshipVO;
import org.fiware.ngsi.model.UpdateResultVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@code replaceOnUpdate} update path: PATCH /attrs with only the attributes of the update, each one
 * replacing the stored attribute.
 */
class NgsiLdBaseRepositoryUpdateTest {

	private static final String ENTITY_ID = "urn:ngsi-ld:product-offering:8de1a1f4-2e56-4e3e-8d5b-8a3fa4a0e6d1";
	private static final String NGSI_LD_NULL = "urn:ngsi-ld:null";

	private EntitiesApiClient entitiesApi;
	private EntityAttributesApiClient entityAttributesApi;
	private JavaObjectMapper javaObjectMapper;
	private NgsiLdEntityRepository repository;

	@BeforeEach
	public void setUp() {
		GeneralProperties properties = new GeneralProperties();
		properties.setReplaceOnUpdate(true);
		entitiesApi = mock(EntitiesApiClient.class);
		entityAttributesApi = mock(EntityAttributesApiClient.class);
		javaObjectMapper = mock(JavaObjectMapper.class);
		NGSIMapper ngsiMapper = new NGSIMapper() {
		};
		repository = new NgsiLdEntityRepository(properties, entitiesApi, null, null, ngsiMapper, javaObjectMapper);
		repository.setEntityAttributesApi(entityAttributesApi);
		when(entitiesApi.updateEntityAttributes(any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.noContent()));
	}

	@Test
	public void aShorterListDeletesTheItemsItDoesNotCarry() {
		// a list of three updated to two of them has to end with exactly those two
		mockStored(Map.of("category", List.of(storedRelationship("c1"), storedRelationship("c2"),
				storedRelationship("c3"))));
		mockUpdate("category", relationships("c1", "c3"));

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		assertEquals(Set.of("write c1", "write c3", "delete c2"), writtenInstances("category"));
	}

	@Test
	public void anEmptyListDeletesTheAttribute() {
		mockStored(Map.of("category", List.of(storedRelationship("c1"), storedRelationship("c2"))));
		mockUpdate("category", new RelationshipListVO());

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		assertEquals(Set.of("delete c1", "delete c2"), writtenInstances("category"));
	}

	@Test
	public void anEmptyListOfAnAttributeThatIsNotStoredIsNotSent() {
		// an empty array is not valid NGSI-LD input, and there is nothing to delete
		mockStored(Map.of());
		mockUpdate("category", new RelationshipListVO());

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		assertFalse(writtenFragment().getAdditionalProperties().containsKey("category"));
	}

	@ParameterizedTest(name = "An emptied {0} has to delete the stored instances.")
	@MethodSource("emptiedLists")
	public void anEmptiedListDeletesTheAttribute(String description, AdditionalPropertyVO emptiedList) {
		// UPDATE mode marks a list emptied by the update with a single NGSI-LD Null, not with an empty array
		mockStored(Map.of("category", List.of(storedRelationship("c1"), storedRelationship("c2"))));
		mockUpdate("category", emptiedList);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		assertEquals(Set.of("delete c1", "delete c2"), writtenInstances("category"));
	}

	@ParameterizedTest(name = "An emptied {0} of an attribute that is not stored must not be sent.")
	@MethodSource("emptiedLists")
	public void anEmptiedListOfAnAttributeThatIsNotStoredIsNotSent(String description,
			AdditionalPropertyVO emptiedList) {
		// the marker would delete an attribute the entity does not have, which the broker answers with a 207
		mockStored(Map.of());
		mockUpdate("category", emptiedList);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		assertFalse(writtenFragment().getAdditionalProperties().containsKey("category"));
	}

	private static Stream<Arguments> emptiedLists() {
		PropertyListVO properties = new PropertyListVO();
		properties.add(new PropertyVO().value(NGSI_LD_NULL));
		RelationshipListVO relationships = new RelationshipListVO();
		relationships.add(new RelationshipVO()._object(URI.create(NGSI_LD_NULL)));
		return Stream.of(Arguments.of("property list", properties), Arguments.of("relationship list", relationships));
	}

	@Test
	public void storedPropertyInstancesWithOtherDatasetIdsAreDeleted() {
		// property lists get random datasetIds on every mapping, so none of the stored ones is kept
		mockStored(Map.of("productOfferingTerm", List.of(storedProperty("old-1"), storedProperty("old-2"))));
		PropertyListVO terms = new PropertyListVO();
		terms.add(new PropertyVO().value("t1").datasetId(URI.create("urn:new-1")));
		mockUpdate("productOfferingTerm", terms);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		assertEquals(Set.of("write urn:new-1", "delete urn:old-1", "delete urn:old-2"),
				writtenInstances("productOfferingTerm"));
	}

	@Test
	public void theDeletionHasTheTypeOfTheStoredInstance() {
		// brokers reject a Relationship carrying its Null in "value"
		mockStored(Map.of("category", List.of(storedRelationship("c1")),
				"productOfferingTerm", List.of(storedProperty("old-1"))));
		mockUpdate(Map.of("category", new RelationshipListVO(), "productOfferingTerm", new PropertyListVO()));

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		RelationshipVO relationshipDeletion = (RelationshipVO) writtenList("category").get(0);
		assertEquals(URI.create(NGSI_LD_NULL), relationshipDeletion.getObject());
		PropertyVO propertyDeletion = (PropertyVO) writtenList("productOfferingTerm").get(0);
		assertEquals(NGSI_LD_NULL, propertyDeletion.getValue());
	}

	@Test
	public void aStoredDefaultInstanceIsDeleted() {
		// entities written before the datasetIds were introduced hold an instance without one
		Map<String, Object> defaultInstance = new HashMap<>(storedRelationship("c0"));
		defaultInstance.remove("datasetId");
		mockStored(Map.of("category", defaultInstance));
		mockUpdate("category", relationships("c1"));

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		assertEquals(Set.of("write c1", "delete default"), writtenInstances("category"));
	}

	@Test
	public void attributesThatOnlyOverwriteAreSentAsTheyAre() {
		mockStored(Map.of("name", Map.of("type", "Property", "value", "old")));
		PropertyVO name = new PropertyVO().value("new");
		mockUpdate("name", name);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		assertSame(name, writtenFragment().getAdditionalProperties().get("name"));
	}

	@Test
	public void attributesThatAreNotPartOfTheUpdateAreNotSent() {
		mockStored(Map.of("name", Map.of("type", "Property", "value", "old"),
				"category", List.of(storedRelationship("c1"))));
		mockUpdate("description", new PropertyVO().value("new"));

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		assertEquals(Set.of("description"), writtenFragment().getAdditionalProperties().keySet());
	}

	@Test
	public void anUnknownEntityIsAnError() {
		when(entityAttributesApi.retrieveEntityAsJson(eq(URI.create(ENTITY_ID)), any(), any()))
				.thenReturn(Mono.error(new HttpClientResponseException("Not Found",
						HttpResponse.status(HttpStatus.NOT_FOUND))));
		mockUpdate("name", new PropertyVO().value("new"));

		StorageException exception = assertThrows(StorageException.class,
				() -> repository.updateDomainEntity(ENTITY_ID, new Object()).block());

		assertEquals(StorageOperation.READ, exception.operation());
		assertEquals(StorageErrorReason.NOT_FOUND, exception.reason());
		assertEquals(String.format("Entity %s does not exist.", ENTITY_ID), exception.getMessage());
	}

	@Test
	public void aMultiStatusAnswerIsAnError() {
		// the broker lists the attributes it did not write, so the update must not look successful
		mockStored(Map.of());
		mockUpdate("name", new PropertyVO().value("new"));
		when(entitiesApi.updateEntityAttributes(any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.status(HttpStatus.MULTI_STATUS).body((Object) new UpdateResultVO()
						.notUpdated(List.of(new NotUpdatedDetailsVO().attributeName("name").reason("invalid value"))))));

		StorageException exception = assertThrows(StorageException.class,
				() -> repository.updateDomainEntity(ENTITY_ID, new Object()).block());

		assertEquals(StorageOperation.UPDATE, exception.operation());
		assertEquals(StorageErrorReason.PARTIAL_UPDATE, exception.reason());
		assertTrue(exception.getMessage().startsWith("Not all attributes could be updated: "));
		assertTrue(exception.getMessage().contains("name: invalid value"),
				"The attributes the broker did not update have to be part of the error.");
	}

	@Test
	public void aMultiStatusAnswerCompletesTheCacheInvalidatingWrite() {
		// the broker wrote a part of the update, so the cached entity is stale. @CacheInvalidate only acts on a
		// completing mono, therefore the 207 must not fail the write itself, only the update around it.
		when(entitiesApi.updateEntityAttributes(any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.status(HttpStatus.MULTI_STATUS).body((Object) new UpdateResultVO())));

		HttpResponse<?> response = repository.updateAttributes(URI.create(ENTITY_ID), new EntityFragmentVO()).block();

		assertEquals(HttpStatus.MULTI_STATUS, response.getStatus());
	}

	@Test
	public void aMultiStatusAnswerRaisedAsExceptionIsAnError() {
		mockStored(Map.of());
		mockUpdate("name", new PropertyVO().value("new"));
		when(entitiesApi.updateEntityAttributes(any(), any(), any()))
				.thenReturn(Mono.error(new HttpClientResponseException("Multi-Status",
						HttpResponse.status(HttpStatus.MULTI_STATUS))));

		StorageException exception = assertThrows(StorageException.class,
				() -> repository.updateDomainEntity(ENTITY_ID, new Object()).block());

		assertEquals(StorageOperation.UPDATE, exception.operation());
		assertEquals(StorageErrorReason.PARTIAL_UPDATE, exception.reason());
		assertTrue(exception.getMessage().startsWith("Not all attributes could be updated: "));
	}

	@Test
	public void aNoContentAnswerRaisedAsExceptionIsASuccess() {
		// a status other than the declared one surfaces as exception, even if it is a success
		mockStored(Map.of());
		mockUpdate("name", new PropertyVO().value("new"));
		when(entitiesApi.updateEntityAttributes(any(), any(), any()))
				.thenReturn(Mono.error(new HttpClientResponseException("No Content",
						HttpResponse.status(HttpStatus.NO_CONTENT))));

		assertDoesNotThrow(() -> repository.updateDomainEntity(ENTITY_ID, new Object()).block());
	}

	@Test
	public void aBrokerErrorIsPropagated() {
		// the controller maps the reason (INVALID_DATA, CONFLICT, ...) to the TMForum error
		mockStored(Map.of());
		mockUpdate("name", new PropertyVO().value("new"));
		when(entitiesApi.updateEntityAttributes(any(), any(), any()))
				.thenReturn(Mono.error(new HttpClientResponseException("Bad Request",
						HttpResponse.status(HttpStatus.BAD_REQUEST))));

		StorageException exception = assertThrows(StorageException.class,
				() -> repository.updateDomainEntity(ENTITY_ID, new Object()).block());

		assertEquals(StorageOperation.UPDATE, exception.operation());
		assertEquals(StorageErrorReason.INVALID_DATA, exception.reason());
		assertEquals("Bad Request", exception.getMessage());
	}

	private void mockStored(Map<String, Object> attributes) {
		Map<String, Object> entity = new HashMap<>(attributes);
		entity.put("id", ENTITY_ID);
		entity.put("type", "product-offering");
		when(entityAttributesApi.retrieveEntityAsJson(eq(URI.create(ENTITY_ID)), any(), any()))
				.thenReturn(Mono.just(HttpResponse.ok(entity)));
	}

	private void mockUpdate(String attribute, AdditionalPropertyVO value) {
		mockUpdate(Map.of(attribute, value));
	}

	private void mockUpdate(Map<String, AdditionalPropertyVO> attributes) {
		// what the domain object to be updated maps to - a partial entity carrying just the change
		EntityVO update = new EntityVO();
		update.setId(URI.create(ENTITY_ID));
		update.setType("product-offering");
		attributes.forEach(update::setAdditionalProperties);
		when(javaObjectMapper.toEntityVO(any(), eq(NgsiLdWriteOperation.UPDATE))).thenReturn(update);
	}

	private static Map<String, Object> storedRelationship(String category) {
		return Map.of("type", "Relationship", "object", category, "datasetId", category);
	}

	private static Map<String, Object> storedProperty(String datasetId) {
		return Map.of("type", "Property", "value", Map.of("name", datasetId), "datasetId", "urn:" + datasetId);
	}

	private static RelationshipListVO relationships(String... categories) {
		RelationshipListVO relationships = new RelationshipListVO();
		for (String category : categories) {
			relationships.add(new RelationshipVO()._object(URI.create(category)).datasetId(URI.create(category)));
		}
		return relationships;
	}

	private EntityFragmentVO writtenFragment() {
		ArgumentCaptor<EntityFragmentVO> captor = ArgumentCaptor.forClass(EntityFragmentVO.class);
		verify(entitiesApi).updateEntityAttributes(eq(URI.create(ENTITY_ID)), captor.capture(), any());
		return captor.getValue();
	}

	private List<?> writtenList(String attribute) {
		Object written = writtenFragment().getAdditionalProperties().get(attribute);
		assertTrue(written instanceof List<?>, "Expected the instances of " + attribute + ", but was " + written);
		return (List<?>) written;
	}

	/**
	 * The written instances as "write &lt;datasetId&gt;" or "delete &lt;datasetId&gt;", "default" for none.
	 */
	private Set<String> writtenInstances(String attribute) {
		return writtenList(attribute).stream().map(instance -> {
			URI datasetId;
			boolean deletion;
			if (instance instanceof RelationshipVO relationshipVO) {
				datasetId = relationshipVO.getDatasetId();
				deletion = URI.create(NGSI_LD_NULL).equals(relationshipVO.getObject());
			} else {
				PropertyVO propertyVO = (PropertyVO) instance;
				datasetId = propertyVO.getDatasetId();
				deletion = NGSI_LD_NULL.equals(propertyVO.getValue());
			}
			return (deletion ? "delete " : "write ") + (datasetId == null ? "default" : datasetId.toString());
		}).collect(Collectors.toSet());
	}
}
