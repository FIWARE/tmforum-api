package org.fiware.tmforum.party.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.Ignore;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.domain.organization.Organization;

import java.util.List;

@MappingEnabled(entityType = {Individual.TYPE_INDIVIDUAL, Organization.TYPE_ORGANIZATION})
@EqualsAndHashCode(callSuper = true)
public class RelatedParty extends RefEntity {

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name", targetClass = String.class)}))
	private String name;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "role")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "role", targetClass = String.class)}))
	private String role;

	public RelatedParty(String id) {
		super(id);
	}

	@Override
	@Ignore
	public List<String> getReferencedTypes() {
		return List.of(Organization.TYPE_ORGANIZATION, Individual.TYPE_INDIVIDUAL);
	}
}
