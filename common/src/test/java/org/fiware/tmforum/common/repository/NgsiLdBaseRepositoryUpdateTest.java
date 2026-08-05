package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.http.HttpResponse;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.BatchOperationResultVO;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PropertyListVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the read-merge-write update path ({@code replaceOnUpdate}, required for Scorpio 6.x).
 */
class NgsiLdBaseRepositoryUpdateTest {

	private static final String ENTITY_ID = "urn:ngsi-ld:product-offering-price:8de1a1f4-2e56-4e3e-8d5b-8a3fa4a0e6d1";

	private GeneralProperties properties;
	private EntitiesApiClient entitiesApi;
	private JavaObjectMapper javaObjectMapper;
	private TmForumRepository repository;

	@BeforeEach
	public void setUp() {
		properties = new GeneralProperties();
		properties.setReplaceOnUpdate(true);
		entitiesApi = mock(EntitiesApiClient.class);
		javaObjectMapper = mock(JavaObjectMapper.class);
		repository = new TmForumRepository(properties, entitiesApi, null, null, null, javaObjectMapper);
		when(entitiesApi.batchEntityUpsert(any(), any()))
				.thenReturn(Mono.just(HttpResponse.ok(new BatchOperationResultVO())));
	}

	@Test
	public void reservedWordsAreEscapedAgainBeforeTheEntityIsWrittenBack() {
		// The EscapeCleaningParser of the mapping library strips the tmfEscaped- prefix from
		// reserved words while the existing entity is read from the broker. Writing that back
		// verbatim puts raw JSON-LD keywords on the wire, which Scorpio >= 6.0.0 drops - and with
		// them the expanded ODRL policy of the offering price, which then fails to map in the EDC.
		EntityVO existing = anEntity();
		PropertyVO policy = aProperty(Map.of(
				"@type", List.of("http://www.w3.org/ns/odrl/2/Offer"),
				"@id", "urn:uuid:1b0f3b8a-6c2a-4b6b-9d0a-2f0f6a1a3c4d"));
		existing.setAdditionalProperties("policy", policy);
		mockRetrieval(existing);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		Map<String, Object> writtenPolicy = writtenValueOf("policy");
		assertEquals(Map.of(
						"tmfEscaped-@type", List.of("http://www.w3.org/ns/odrl/2/Offer"),
						"tmfEscaped-@id", "urn:uuid:1b0f3b8a-6c2a-4b6b-9d0a-2f0f6a1a3c4d"),
				writtenPolicy,
				"Keywords the parser unescaped on read have to be escaped again on write.");
	}

	@Test
	public void escapingIsAppliedRecursivelyAndIsIdempotent() {
		// Nested objects, lists and the keys the parser deliberately leaves escaped
		// (id/type/value collide with VO fields) all have to end up correct.
		EntityVO existing = anEntity();
		existing.setAdditionalProperties("policy", aProperty(Map.of(
				"@type", "Offer",
				"tmfEscaped-id", "already-escaped",
				"permission", List.of(Map.of(
						"@id", "urn:uuid:permission",
						"constraint", Map.of("@value", "5"))))));
		mockRetrieval(existing);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		Map<String, Object> written = writtenValueOf("policy");
		assertEquals("Offer", written.get("tmfEscaped-@type"));
		assertEquals("already-escaped", written.get("tmfEscaped-id"),
				"Keys that kept their prefix during parsing must not be escaped twice.");
		Map<String, Object> permission = (Map<String, Object>) ((List<Object>) written.get("permission")).get(0);
		assertEquals("urn:uuid:permission", permission.get("tmfEscaped-@id"));
		assertEquals(Map.of("tmfEscaped-@value", "5"), permission.get("constraint"));
	}

	@Test
	public void escapingCoversSubAttributesAndMultiAttributes() {
		EntityVO existing = anEntity();
		PropertyVO first = aProperty("first");
		first.setAdditionalProperties("@type", aProperty("sub-attribute"));
		PropertyVO second = aProperty(Map.of("@id", "urn:uuid:second"));
		PropertyListVO multiAttribute = new PropertyListVO();
		multiAttribute.add(first);
		multiAttribute.add(second);
		existing.setAdditionalProperties("relatedParty", multiAttribute);
		mockRetrieval(existing);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		PropertyListVO written = (PropertyListVO) writtenEntity().getAdditionalProperties().get("relatedParty");
		assertTrue(written.get(0).getAdditionalProperties().containsKey("tmfEscaped-@type"),
				"Sub-attribute names are escaped as well.");
		assertFalse(written.get(0).getAdditionalProperties().containsKey("@type"));
		assertEquals(Map.of("tmfEscaped-@id", "urn:uuid:second"), written.get(1).getValue());
	}

	@Test
	public void anUpdateWithoutAdditionalPropertiesDoesNotFail() {
		// e.g. an update that only touches a structural field. The merge used to NPE here.
		EntityVO existing = anEntity();
		existing.setAdditionalProperties("name", aProperty("an offering price"));
		when(javaObjectMapper.toEntityVO(any())).thenReturn(new EntityVO());
		when(entitiesApi.retrieveEntityById(eq(URI.create(ENTITY_ID)), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.ok(existing)));

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		assertEquals("an offering price",
				((PropertyVO) writtenEntity().getAdditionalProperties().get("name")).getValue());
	}

	@Test
	public void plainAttributesAreLeftAlone() {
		EntityVO existing = anEntity();
		existing.setAdditionalProperties("name", aProperty("an offering price"));
		mockRetrieval(existing);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		EntityVO written = writtenEntity();
		assertEquals("an offering price", ((PropertyVO) written.getAdditionalProperties().get("name")).getValue());
		assertEquals("2026-08-05T10:15:30Z",
				((PropertyVO) written.getAdditionalProperties().get("lastUpdate")).getValue(),
				"The update still has to be merged on top of the existing entity.");
	}

	private EntityVO anEntity() {
		EntityVO entityVO = new EntityVO();
		entityVO.setId(URI.create(ENTITY_ID));
		entityVO.setType("product-offering-price");
		return entityVO;
	}

	private PropertyVO aProperty(Object value) {
		PropertyVO propertyVO = new PropertyVO();
		// the mapping library parses free-form values into mutable maps, mirror that
		propertyVO.setValue(value instanceof Map<?, ?> map ? new LinkedHashMap<>(map) : value);
		return propertyVO;
	}

	private void mockRetrieval(EntityVO existing) {
		// what the domain object to be updated maps to - a partial entity carrying just the change
		EntityVO update = new EntityVO();
		update.setAdditionalProperties("lastUpdate", aProperty("2026-08-05T10:15:30Z"));
		when(javaObjectMapper.toEntityVO(any())).thenReturn(update);
		when(entitiesApi.retrieveEntityById(eq(URI.create(ENTITY_ID)), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.ok(existing)));
	}

	private EntityVO writtenEntity() {
		ArgumentCaptor<EntityListVO> captor = ArgumentCaptor.forClass(EntityListVO.class);
		verify(entitiesApi).batchEntityUpsert(captor.capture(), eq("replace"));
		return captor.getValue().get(0);
	}

	private Map<String, Object> writtenValueOf(String attribute) {
		return (Map<String, Object>) ((PropertyVO) writtenEntity().getAdditionalProperties().get(attribute)).getValue();
	}
}
