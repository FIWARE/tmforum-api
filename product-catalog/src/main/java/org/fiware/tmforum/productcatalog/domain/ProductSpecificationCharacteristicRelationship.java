package org.fiware.tmforum.productcatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.party.domain.TimePeriod;

import java.util.List;

@MappingEnabled(entityType = ProductSpecificationCharacteristic.TYPE_PRODUCT_SPECIFICATION_CHARACTERISTIC)
@EqualsAndHashCode(callSuper = true)
public class ProductSpecificationCharacteristicRelationship extends RefEntity {

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "charSpecSeq")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "charSpecSeq")}))
    private Integer charSpecSeq;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType")}))
    private String relationshipType;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
    private TimePeriod validFor;

    public ProductSpecificationCharacteristicRelationship(String id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(ProductSpecificationCharacteristic.TYPE_PRODUCT_SPECIFICATION_CHARACTERISTIC);
    }
}
