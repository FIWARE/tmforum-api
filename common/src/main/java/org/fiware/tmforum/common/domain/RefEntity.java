package org.fiware.tmforum.common.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.DatasetId;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.mapping.annotations.RelationshipObject;

import java.net.URI;

@EqualsAndHashCode
public abstract class RefEntity extends Entity implements ReferencedEntity {

	@Getter(onMethod = @__({@RelationshipObject, @DatasetId}))
	final URI id;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href", embedProperty = true)}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href", fromProperties = true)}))
	private URI href;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name", embedProperty = true)}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name", fromProperties = true)}))
	private String name;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "@referredType", embedProperty = true)}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "@referredType", fromProperties = true)}))
	private String atReferredType;

	protected RefEntity(String id) {
		this.id = URI.create(id);
	}

	protected RefEntity(URI id) {
		this.id = id;
	}

}
