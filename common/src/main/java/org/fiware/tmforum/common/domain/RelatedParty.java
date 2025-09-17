package org.fiware.tmforum.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.annotations.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

@MappingEnabled(entityType = {"organization", "individual"})
@EqualsAndHashCode(callSuper = true)
public class RelatedParty extends RefEntity {

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name", embedProperty = true)}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name", targetClass = String.class)}))
	private String name;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "role", embedProperty = true)}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "role", targetClass = String.class)}))
	private String role;

	public RelatedParty(@JsonProperty("id") String id) {
		super(id);
	}

	@Override
	@JsonIgnore
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of("organization", "individual"));
	}

	@RelationshipObject
	@Override
	public URI getId() {
		return id;
	}

	@DatasetId
	public URI getDatasetId() {
		String role = Optional.ofNullable(getRole()).orElse("role");

		return URI.create(String.format("%s:%s", id.toString(), role));
	}
}
