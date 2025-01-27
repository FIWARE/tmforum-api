package org.fiware.tmforum.agreement.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.RefEntity;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@MappingEnabled(entityType = {AgreementSpecification.TYPE_AGREEMENT_SPECIFICATION})
@EqualsAndHashCode(callSuper = true)
public class AgreementSpecificationRef extends RefEntity {

	public AgreementSpecificationRef(@JsonProperty("id") String id) {
		super(id);
	}

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description", embedProperty = true)}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description", targetClass = String.class)}))
	private String description;

	@Override
	@JsonIgnore
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(AgreementSpecification.TYPE_AGREEMENT_SPECIFICATION));
	}

}
