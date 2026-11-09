package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JsonLdContext;
import io.github.wistefan.mapping.MappingException;
import io.github.wistefan.mapping.MappingProperties;
import io.github.wistefan.mapping.NgsiLdKeyCompactor;
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
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins that references read back from their own sub-properties without a target lookup, and that a relationship
 * still asking for its target fails the read.
 */
public class InlineReferencesEntitiesRepositoryTest {

	private static final String PARENT_TYPE = "parent";
	private static final String CHILD_TYPE = "child";
	private static final String CHILDREN = "children";
	private static final String FAVOURITE = "favourite";
	private static final String INLINE_CHILDREN = "inlineChildren";
	private static final String INLINE_FAVOURITE = "inlineFavourite";
	private static final String NAME = "name";
	static final String REMOTE_NAME = "remote child";
	// a deployment context's IRI for "name", which Orion-LD returns for the sub-attributes of a multi-instance list
	static final String CUSTOM_NAME_IRI = "https://context.example.org/terms/name";

	private static final URI PARENT = URI.create("urn:ngsi-ld:parent:p");
	private static final URI CHILD_A = URI.create("urn:ngsi-ld:child:a");
	private static final URI CHILD_B = URI.create("urn:ngsi-ld:child:b");

	private final InlineReferencesEntitiesRepository repository = new InlineReferencesEntitiesRepository();

	@Test
	public void getEntities_emptyRequestIsEmpty() {
		assertEquals(List.of(), repository.getEntities(List.of()).block());
	}

	@Test
	public void getEntities_failsNamingTheRequestedIds() {
		MappingException exception = assertThrows(MappingException.class,
				() -> repository.getEntities(List.of(CHILD_A, CHILD_B)).block());

		assertTrue(exception.getMessage().contains(CHILD_A.toString()), exception.getMessage());
		assertTrue(exception.getMessage().contains(CHILD_B.toString()), exception.getMessage());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("inlineLists")
	public void fromEntityVO_readsInlineListsFromTheirSubProperties(String description, List<URI> children) {
		RelationshipListVO relationshipList = new RelationshipListVO();
		children.forEach(id -> relationshipList.add(namedRelationship(id, NAME).datasetId(id)));

		Parent parent = mapper(false, new NgsiLdKeyCompactor())
				.fromEntityVO(parent(INLINE_CHILDREN, relationshipList), Parent.class).block();

		assertEquals(children, parent.getInlineChildren().stream().map(Child::getId).toList(), description);
		parent.getInlineChildren().forEach(child -> assertEquals(REMOTE_NAME, child.getName(), description));
	}

	private static Stream<Arguments> inlineLists() {
		return Stream.of(
				Arguments.of("one entry", List.of(CHILD_A)),
				Arguments.of("two entries", List.of(CHILD_A, CHILD_B)));
	}

	@Test
	public void fromEntityVO_readsAnInlineSingleFromItsSubProperties() {
		Parent parent = mapper(false, new NgsiLdKeyCompactor())
				.fromEntityVO(parent(INLINE_FAVOURITE, namedRelationship(CHILD_A, NAME)), Parent.class).block();

		assertEquals(CHILD_A, parent.getInlineFavourite().getId());
		assertEquals(REMOTE_NAME, parent.getInlineFavourite().getName());
	}

	@Test
	public void fromEntityVO_compactsSubAttributesExpandedUnderTheConfiguredContext() {
		NgsiLdKeyCompactor keyCompactor = new NgsiLdKeyCompactor(
				new JsonLdContext(Map.of(CUSTOM_NAME_IRI, NAME), List.of()));

		Parent parent = mapper(false, keyCompactor).fromEntityVO(parentWithExpandedInlineChildren(), Parent.class)
				.block();

		assertEquals(List.of(REMOTE_NAME, REMOTE_NAME), parent.getInlineChildren().stream().map(Child::getName).toList());
	}

	@ParameterizedTest(name = "strictRelationships={0}")
	@ValueSource(booleans = {true, false})
	public void fromEntityVO_relationshipThatResolvesFailsTheRead(boolean strictRelationships) {
		RelationshipListVO relationshipList = new RelationshipListVO();
		relationshipList.add(new RelationshipVO()._object(CHILD_A));

		Mono<Parent> list = mapper(strictRelationships, new NgsiLdKeyCompactor())
				.fromEntityVO(parent(CHILDREN, relationshipList), Parent.class);
		Mono<Parent> single = mapper(strictRelationships, new NgsiLdKeyCompactor())
				.fromEntityVO(parent(FAVOURITE, new RelationshipVO()._object(CHILD_A)), Parent.class);

		assertThrows(MappingException.class, list::block);
		assertThrows(MappingException.class, single::block);
	}

	private EntityVOMapper mapper(boolean strictRelationships, NgsiLdKeyCompactor keyCompactor) {
		MappingProperties mappingProperties = new MappingProperties();
		mappingProperties.setStrictRelationships(strictRelationships);
		JsonMapper objectMapper = JsonMapper.builder().addModule(new NgsiLdMappingModule()).build();
		return new EntityVOMapper(mappingProperties, objectMapper, repository, keyCompactor);
	}

	private static EntityVO parent(String attribute, AdditionalPropertyVO value) {
		EntityVO parent = new EntityVO().id(PARENT).type(PARENT_TYPE);
		parent.setAdditionalProperties(attribute, value);
		return parent;
	}

	private static RelationshipVO namedRelationship(URI target, String nameKey) {
		RelationshipVO relationship = new RelationshipVO()._object(target);
		relationship.setAdditionalProperties(nameKey, new PropertyVO().value(REMOTE_NAME));
		return relationship;
	}

	// two inline children whose "name" arrives expanded under CUSTOM_NAME_IRI
	static EntityVO parentWithExpandedInlineChildren() {
		RelationshipListVO relationshipList = new RelationshipListVO();
		relationshipList.add(namedRelationship(CHILD_A, CUSTOM_NAME_IRI).datasetId(CHILD_A));
		relationshipList.add(namedRelationship(CHILD_B, CUSTOM_NAME_IRI).datasetId(CHILD_B));
		return parent(INLINE_CHILDREN, relationshipList);
	}

	/** Entity holding relationships that resolve their target and relationships read from properties. */
	@MappingEnabled(entityType = PARENT_TYPE)
	public static class Parent {

		private List<Child> children;
		private Child favourite;
		private List<Child> inlineChildren;
		private Child inlineFavourite;

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

		public List<Child> getInlineChildren() {
			return inlineChildren;
		}

		@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = INLINE_CHILDREN, targetClass = Child.class,
				fromProperties = true)
		public void setInlineChildren(List<Child> inlineChildren) {
			this.inlineChildren = inlineChildren;
		}

		public Child getInlineFavourite() {
			return inlineFavourite;
		}

		@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = INLINE_FAVOURITE, targetClass = Child.class,
				fromProperties = true)
		public void setInlineFavourite(Child inlineFavourite) {
			this.inlineFavourite = inlineFavourite;
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
