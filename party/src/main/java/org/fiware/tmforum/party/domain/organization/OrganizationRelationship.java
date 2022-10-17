package org.fiware.tmforum.party.domain.organization;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.List;

@EqualsAndHashCode
public abstract class OrganizationRelationship extends RefEntity {

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType", embedProperty = true)}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType", targetClass = String.class)}))
	private String relationshipType;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name", embedProperty = true)}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name", targetClass = String.class)}))
	private String name;

	protected OrganizationRelationship(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(Organization.TYPE_ORGANIZATION);
	}
}


