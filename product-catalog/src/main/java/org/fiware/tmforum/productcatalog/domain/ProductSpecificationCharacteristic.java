package org.fiware.tmforum.productcatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.util.List;

// TODO: make relationship

@MappingEnabled(entityType = ProductSpecificationCharacteristic.TYPE_PRODUCT_SPECIFICATION_CHARACTERISTIC)
@EqualsAndHashCode(callSuper = true)
public class ProductSpecificationCharacteristic extends EntityWithId {

	public static final String TYPE_PRODUCT_SPECIFICATION_CHARACTERISTIC = "product-specification-characteristic";

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "configurable")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "configurable")}))
	private boolean configurable;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description")}))
	private String description;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "@valueSchemaLocation")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "@valueSchemaLocation")}))
	private String atValueSchemaLocation;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "extensible")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "extensible")}))
	private boolean extensible;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "isUnique")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "isUnique")}))
	private boolean isUnique;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "maxCardinality")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "maxCardinality")}))
	private int maxCardinality;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "minCardinality")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "minCardinality")}))
	private int minCardinality;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
	private String name;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "regex")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "regex")}))
	private String regex;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "valueType")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "valueType")}))
	private String valueType;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "productSpecCharRelationship")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "productSpecCharRelationship", targetClass = ProductSpecificationCharacteristicRelationship.class)}))
	private List<ProductSpecificationCharacteristicRelationship> productSpecCharRelationship;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "productSpecCharacteristicValue")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "productSpecCharacteristicValue")}))
	private List<CharacteristicValueSpecification> productSpecCharacteristicValue;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
	private TimePeriod validFor;

	public ProductSpecificationCharacteristic(String id) {
		super(TYPE_PRODUCT_SPECIFICATION_CHARACTERISTIC, id);
	}
}
