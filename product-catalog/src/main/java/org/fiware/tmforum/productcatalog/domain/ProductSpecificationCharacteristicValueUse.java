package org.fiware.tmforum.productcatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.util.List;

@MappingEnabled(entityType = ProductSpecificationCharacteristicValueUse.TYPE_PRODUCT_SPEC_CHARACTERISTIC_VALUE_USE)
@EqualsAndHashCode(callSuper = true)
public class ProductSpecificationCharacteristicValueUse extends EntityWithId implements ReferencedEntity {

    public static final String TYPE_PRODUCT_SPEC_CHARACTERISTIC_VALUE_USE = "prod-spec-characteristic-value-use";

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description")}))
    private String description;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "maxCardinality")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "maxCardinality")}))
    private Integer maxCardinality;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "minCardinality")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "minCardinality")}))
    private Integer minCardinality;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "valueType")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "valueType")}))
    private String valueType;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "productSpecCharacteristicValue")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "productSpecCharacteristicValue")}))
    private List<CharacteristicValueSpecification> productSpecCharacteristicValue;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "productSpecification")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "productSpecification", targetClass = ProductSpecificationRef.class)}))
    private ProductSpecificationRef productSpecification;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
    private TimePeriod validFor;

    public ProductSpecificationCharacteristicValueUse(String id) {
        super(TYPE_PRODUCT_SPEC_CHARACTERISTIC_VALUE_USE, id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(TYPE_PRODUCT_SPEC_CHARACTERISTIC_VALUE_USE);
    }
}
