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

import java.net.URI;
import java.net.URL;
import java.util.List;

@MappingEnabled(entityType = BundleProductSpecification.TYPE_BUNDLE_PRODUCT_SPEC)
@EqualsAndHashCode(callSuper = true)
public class BundleProductSpecification extends EntityWithId implements ReferencedEntity {

    public static final String TYPE_BUNDLE_PRODUCT_SPEC = "bundle-product-specification";

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URI href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus")}))
    private String lifecycleStatus;

    public BundleProductSpecification(String id) {
        super(TYPE_BUNDLE_PRODUCT_SPEC, id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return null;
    }
}
