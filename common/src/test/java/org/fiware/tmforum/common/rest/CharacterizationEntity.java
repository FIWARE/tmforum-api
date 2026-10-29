package org.fiware.tmforum.common.rest;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.EntityWithId;

import java.util.List;

/**
 * Entity of {@link CharacterizationTestController}: a property and a relationship that is resolved on read.
 */
@MappingEnabled(entityType = CharacterizationEntity.TYPE)
public class CharacterizationEntity extends EntityWithId {

	public static final String TYPE = "characterization-entity";
	public static final String RELATED = "related";

	private String name;
	private List<CharacterizationRelated> related;

	public CharacterizationEntity(String id) {
		super(TYPE, id);
	}

	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")
	public String getName() {
		return name;
	}

	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")
	public void setName(String name) {
		this.name = name;
	}

	@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = RELATED)
	public List<CharacterizationRelated> getRelated() {
		return related;
	}

	@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = RELATED,
			targetClass = CharacterizationRelated.class)
	public void setRelated(List<CharacterizationRelated> related) {
		this.related = related;
	}
}
