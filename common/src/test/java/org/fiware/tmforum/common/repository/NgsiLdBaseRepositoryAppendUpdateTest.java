package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.NgsiLdWriteOperation;
import io.micronaut.http.HttpResponse;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityFragmentVO;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PropertyListVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipListVO;
import org.fiware.ngsi.model.RelationshipVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the default (Orion-LD) update path: a plain {@code POST /attrs} built from a single
 * mapping of the domain object.
 */
class NgsiLdBaseRepositoryAppendUpdateTest {

	private static final String NGSI_LD_NULL = "urn:ngsi-ld:null";
	private static final String LIST_ATTRIBUTE = "productOfferingTerm";
	private static final String ENTITY_ID = "urn:ngsi-ld:product-offering-price:8de1a1f4-2e56-4e3e-8d5b-8a3fa4a0e6d1";

	private EntitiesApiClient entitiesApi;
	private JavaObjectMapper javaObjectMapper;
	private NgsiLdEntityRepository repository;

	@BeforeEach
	public void setUp() {
		GeneralProperties properties = new GeneralProperties();
		properties.setReplaceOnUpdate(false);
		entitiesApi = mock(EntitiesApiClient.class);
		javaObjectMapper = mock(JavaObjectMapper.class);
		NGSIMapper ngsiMapper = new NGSIMapper() {
		};
		repository = new NgsiLdEntityRepository(properties, entitiesApi, null, null, ngsiMapper, javaObjectMapper);
		when(entitiesApi.updateEntity(any(), any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.noContent()));
	}

	@Test
	public void domainEntityIsOnlyMappedOnce() {
		// used to call javaObjectMapper.toEntityVO a second time inline instead of
		// reusing the already-computed value, wastefully mapping the same object twice.
		EntityVO mapped = anEntity();
		when(javaObjectMapper.toEntityVO(any(), eq(NgsiLdWriteOperation.UPDATE))).thenReturn(mapped);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		verify(javaObjectMapper, times(1)).toEntityVO(any(), eq(NgsiLdWriteOperation.UPDATE));
	}

	@Test
	public void thePatchIsBuiltFromTheSingleMappedEntity() {
		EntityVO mapped = anEntity();
		mapped.setAdditionalProperties("name", null);
		when(javaObjectMapper.toEntityVO(any(), eq(NgsiLdWriteOperation.UPDATE))).thenReturn(mapped);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		ArgumentCaptor<EntityFragmentVO> captor = ArgumentCaptor.forClass(EntityFragmentVO.class);
		verify(entitiesApi).updateEntity(eq(URI.create(ENTITY_ID)), captor.capture(), any(), any());
		assertEquals(mapped.getType(), captor.getValue().getType());
	}

	@ParameterizedTest
	@MethodSource("emptiedLists")
	public void anEmptiedListIsPatchedAsEmptyArray(AdditionalPropertyVO ngsiLdNullList, Class<?> expectedType) {
		// Orion-LD ignores NGSI-LD Null on PATCH /attrs and keeps the attribute, but empties it for `[]`.
		EntityVO mapped = anEntity();
		mapped.setAdditionalProperties(LIST_ATTRIBUTE, ngsiLdNullList);
		when(javaObjectMapper.toEntityVO(any(), eq(NgsiLdWriteOperation.UPDATE))).thenReturn(mapped);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		ArgumentCaptor<EntityFragmentVO> captor = ArgumentCaptor.forClass(EntityFragmentVO.class);
		verify(entitiesApi).updateEntity(eq(URI.create(ENTITY_ID)), captor.capture(), any(), any());
		AdditionalPropertyVO patched = captor.getValue().getAdditionalProperties().get(LIST_ATTRIBUTE);
		assertEquals(expectedType, patched.getClass(), "The list type has to be kept.");
		assertEquals(0, ((List<?>) patched).size(), "An emptied list has to be sent as empty array.");
	}

	private static Stream<Arguments> emptiedLists() {
		PropertyListVO propertyList = new PropertyListVO();
		propertyList.add(new PropertyVO().value(NGSI_LD_NULL));
		RelationshipListVO relationshipList = new RelationshipListVO();
		RelationshipVO nullRelationship = new RelationshipVO();
		nullRelationship.setObject(URI.create(NGSI_LD_NULL));
		relationshipList.add(nullRelationship);
		return Stream.of(
				Arguments.of(propertyList, PropertyListVO.class),
				Arguments.of(relationshipList, RelationshipListVO.class));
	}

	private EntityVO anEntity() {
		EntityVO entityVO = new EntityVO();
		entityVO.setId(URI.create(ENTITY_ID));
		entityVO.setType("product-offering-price");
		return entityVO;
	}
}
