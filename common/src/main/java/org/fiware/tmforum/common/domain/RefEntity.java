package org.fiware.tmforum.common.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.DatasetId;
import io.github.wistefan.mapping.annotations.RelationshipObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.validation.ReferencedEntity;

import java.net.URI;

@EqualsAndHashCode(callSuper = true)
public abstract class RefEntity extends Entity implements ReferencedEntity {

	@Getter(onMethod = @__({ @RelationshipObject, @DatasetId }))
	 final URI id;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href", fromProperties = true, targetClass = URI.class) }))
	private URI href;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name", fromProperties = true) }))
	private String name;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "@referredType", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "@referredType", fromProperties = true) }))
	private String atReferredType;

	protected RefEntity(String id) {
		this.id = URI.create(id);
	}

	protected RefEntity(URI id) {
		this.id = id;
	}

	@Override public URI getEntityId() {
		return getId();
	}
}
