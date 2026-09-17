package org.fiware.tmforum.common.repository;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.http.HttpResponse;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityFragmentVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.NGSIMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the default (non-{@code replaceOnUpdate}, Orion-LD) update path: a plain
 * {@code PATCH /attrs} built from a single mapping of the domain object.
 */
class NgsiLdBaseRepositoryPatchUpdateTest {

	private static final String ENTITY_ID = "urn:ngsi-ld:product-offering-price:8de1a1f4-2e56-4e3e-8d5b-8a3fa4a0e6d1";

	private EntitiesApiClient entitiesApi;
	private JavaObjectMapper javaObjectMapper;
	private TmForumRepository repository;

	@BeforeEach
	public void setUp() {
		GeneralProperties properties = new GeneralProperties();
		properties.setReplaceOnUpdate(false);
		entitiesApi = mock(EntitiesApiClient.class);
		javaObjectMapper = mock(JavaObjectMapper.class);
		NGSIMapper ngsiMapper = new NGSIMapper() {
		};
		repository = new TmForumRepository(properties, entitiesApi, null, null, ngsiMapper, javaObjectMapper);
		when(entitiesApi.updateEntity(any(), any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.noContent()));
	}

	@Test
	public void domainEntityIsOnlyMappedOnce() {
		// used to call javaObjectMapper.toEntityVO(domainEntity) a second time inline instead of
		// reusing the already-computed value, wastefully mapping the same object twice.
		EntityVO mapped = anEntity();
		when(javaObjectMapper.toEntityVO(any())).thenReturn(mapped);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		verify(javaObjectMapper, times(1)).toEntityVO(any());
	}

	@Test
	public void thePatchIsBuiltFromTheSingleMappedEntity() {
		EntityVO mapped = anEntity();
		mapped.setAdditionalProperties("name", null);
		when(javaObjectMapper.toEntityVO(any())).thenReturn(mapped);

		repository.updateDomainEntity(ENTITY_ID, new Object()).block();

		ArgumentCaptor<EntityFragmentVO> captor = ArgumentCaptor.forClass(EntityFragmentVO.class);
		verify(entitiesApi).updateEntity(eq(URI.create(ENTITY_ID)), captor.capture(), any(), any());
		assertEquals(mapped.getType(), captor.getValue().getType());
	}

	private EntityVO anEntity() {
		EntityVO entityVO = new EntityVO();
		entityVO.setId(URI.create(ENTITY_ID));
		entityVO.setType("product-offering-price");
		return entityVO;
	}
}
