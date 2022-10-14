package org.fiware.tmforum.productcatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.List;

@MappingEnabled(entityType = {ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE})
@EqualsAndHashCode(callSuper = true)
public class BundledProductOfferingPriceRelationship extends RefEntity {


    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URI href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    public BundledProductOfferingPriceRelationship(String id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE);
    }
}
