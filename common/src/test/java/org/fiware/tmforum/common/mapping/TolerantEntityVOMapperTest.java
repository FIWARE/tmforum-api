package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.EntitiesRepository;
import io.github.wistefan.mapping.MappingException;
import io.github.wistefan.mapping.MappingProperties;
import io.github.wistefan.mapping.NgsiLdMappingModule;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipListVO;
import org.fiware.ngsi.model.RelationshipVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins that unresolved relationship targets read back id-only, in lists of any length and in single relationships.
 */
public class TolerantEntityVOMapperTest {

	private static final String PARENT_TYPE = "parent";
	private static final String CHILD_TYPE = "child";
	private static final String CHILDREN = "children";
	private static final String FAVOURITE = "favourite";
	private static final String NAME = "name";
	private static final String REMOTE_NAME = "remote child";

	private static final URI PARENT = URI.create("urn:ngsi-ld:parent:p");
	private static final URI FOUND = URI.create("urn:ngsi-ld:child:found");
	private static final URI MISSING_A = URI.create("urn:ngsi-ld:child:missing-a");
	private static final URI MISSING_B = URI.create("urn:ngsi-ld:child:missing-b");

	@ParameterizedTest(name = "{0}")
	@MethodSource("relationshipLists")
	public void fromEntityVO_keepsUnresolvedListEntries(String description, List<URI> children) {
		RelationshipListVO relationshipList = new RelationshipListVO();
		children.forEach(id -> relationshipList.add(relationship(id)));

		Parent parent = mapper(false).fromEntityVO(parent(CHILDREN, relationshipList), Parent.class).block();

		assertEquals(children, parent.getChildren().stream().map(Child::getId).toList(), description);
	}

	private static Stream<Arguments> relationshipLists() {
		return Stream.of(
				Arguments.of("one unresolved entry", List.of(MISSING_A)),
				Arguments.of("unresolved between resolved", List.of(MISSING_A, FOUND, MISSING_B)),
				Arguments.of("all unresolved", List.of(MISSING_A, MISSING_B)));
	}

	@Test
	public void fromEntityVO_unresolvedSingleKeepsSubProperties() {
		RelationshipVO favourite = relationship(MISSING_A);
		favourite.setAdditionalProperties(NAME, new PropertyVO().value(REMOTE_NAME));

		Parent parent = mapper(false).fromEntityVO(parent(FAVOURITE, favourite), Parent.class).block();

		assertEquals(MISSING_A, parent.getFavourite().getId());
		assertEquals(REMOTE_NAME, parent.getFavourite().getName());
	}

	@Test
	public void fromEntityVO_resolvedTargetIsMappedFromTheEntity() {
		Parent parent = mapper(false).fromEntityVO(parent(FAVOURITE, relationship(FOUND)), Parent.class).block();

		assertEquals(FOUND, parent.getFavourite().getId());
		assertNull(parent.getFavourite().getName());
	}

	@Test
	public void fromEntityVO_strictModeFailsOnUnresolvedTarget() {
		RelationshipListVO relationshipList = new RelationshipListVO();
		relationshipList.add(relationship(FOUND));
		relationshipList.add(relationship(MISSING_A));

		Mono<Parent> parent = mapper(true).fromEntityVO(parent(CHILDREN, relationshipList), Parent.class);

		assertThrows(MappingException.class, parent::block);
	}

	private static TolerantEntityVOMapper mapper(boolean strictRelationships) {
		MappingProperties mappingProperties = new MappingProperties();
		mappingProperties.setStrictRelationships(strictRelationships);
		EntitiesRepository entitiesRepository = mock(EntitiesRepository.class);
		// mirrors BrokerBackedEntitiesRepository: one entity per id, a stub for each one that does not exist
		when(entitiesRepository.getEntities(any())).thenAnswer(invocation -> {
			List<URI> ids = invocation.getArgument(0);
			return Mono.just(ids.stream()
					.map(id -> id.equals(FOUND) ? new EntityVO().id(id).type(CHILD_TYPE)
							: TolerantEntityVOMapper.unresolvedTarget(id))
					.toList());
		});
		JsonMapper objectMapper = JsonMapper.builder().addModule(new NgsiLdMappingModule()).build();
		return new TolerantEntityVOMapper(mappingProperties, objectMapper, entitiesRepository);
	}

	private static EntityVO parent(String attribute, AdditionalPropertyVO value) {
		EntityVO parent = new EntityVO().id(PARENT).type(PARENT_TYPE);
		parent.setAdditionalProperties(attribute, value);
		return parent;
	}

	private static RelationshipVO relationship(URI target) {
		return new RelationshipVO()._object(target);
	}

	/** Entity holding a fetched relationship list and a fetched single relationship. */
	@MappingEnabled(entityType = PARENT_TYPE)
	public static class Parent {

		private List<Child> children;
		private Child favourite;

		public Parent(String id) {
		}

		public List<Child> getChildren() {
			return children;
		}

		@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = CHILDREN, targetClass = Child.class)
		public void setChildren(List<Child> children) {
			this.children = children;
		}

		public Child getFavourite() {
			return favourite;
		}

		@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = FAVOURITE, targetClass = Child.class)
		public void setFavourite(Child favourite) {
			this.favourite = favourite;
		}
	}

	/** Relationship target with a sub-property, like a {@code RefEntity}. */
	@MappingEnabled(entityType = CHILD_TYPE)
	public static class Child {

		private final URI id;
		private String name;

		public Child(String id) {
			this.id = URI.create(id);
		}

		public URI getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		@AttributeSetter(value = AttributeType.PROPERTY, targetName = NAME, fromProperties = true)
		public void setName(String name) {
			this.name = name;
		}
	}
}
