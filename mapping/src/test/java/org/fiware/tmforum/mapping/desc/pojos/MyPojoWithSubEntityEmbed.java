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

@EqualsAndHashCode
@MappingEnabled(entityType = "complex-pojo")
public class MyPojoWithSubEntityEmbed {

	@Getter(onMethod = @__({@EntityId}))
	private URI id;

	@Getter(onMethod = @__({@EntityType}))
	private String type = "complex-pojo";

	public MyPojoWithSubEntityEmbed(String id) {
		this.id = URI.create(id);
	}

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "sub-entity")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "sub-entity", fromProperties = true, targetClass = MySubPropertyEntityEmbed.class)}))
	private MySubPropertyEntityEmbed mySubProperty;

}
