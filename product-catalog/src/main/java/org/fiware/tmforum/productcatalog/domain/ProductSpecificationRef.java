package org.fiware.tmforum.productcatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.Ignore;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

@MappingEnabled
@EqualsAndHashCode(callSuper = true)
public class ProductSpecificationRef extends RefEntity {

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "version")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "version")}))
	private String version;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "targetProductSchema")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "targetProductSchema")}))
	private TargetProductSchema targetProductSchema;

	public ProductSpecificationRef(String id) {
		super(id);
	}

	@Override
	@Ignore
	public List<String> getReferencedTypes() {
		return List.of(ProductSpecification.TYPE_PRODUCT_SPECIFICATION);
	}
}
