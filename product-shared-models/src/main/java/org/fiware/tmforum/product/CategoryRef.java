package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled
public class CategoryRef extends RefEntity {

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "version", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "version", fromProperties = true) }))
	private String version;

	public CategoryRef(String id) {
		super(id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return List.of(Category.TYPE_CATEGORY);
	}

}
