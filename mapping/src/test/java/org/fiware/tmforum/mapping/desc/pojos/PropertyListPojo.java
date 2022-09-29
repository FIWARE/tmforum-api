package org.fiware.tmforum.mapping.desc.pojos;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@EqualsAndHashCode
@MappingEnabled(entityType = "list-pojo")
public class PropertyListPojo {

	@Getter(onMethod = @__({@EntityId}))
	private URI id;

	@Getter(onMethod = @__({@EntityType}))
	private String type = "list-pojo";

	public PropertyListPojo(String id) {
		this.id = URI.create(id);
	}

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "mySubProperty")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "mySubProperty", targetClass = MySubProperty.class)}))
	private MySubProperty property;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "myRelationship")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "myRelationship", targetClass = MySubPropertyEntity.class)}))
	private MySubPropertyEntity relationShip;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "mySubPropertyList")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "mySubPropertyList", targetClass = MySubProperty.class)}))
	private List<MySubProperty> propertyList;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "myRelationshipList")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "myRelationshipList", targetClass = MySubPropertyEntity.class)}))
	private List<MySubPropertyEntity> relationshipList;

}
