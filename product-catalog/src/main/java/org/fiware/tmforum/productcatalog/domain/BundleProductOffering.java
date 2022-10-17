package org.fiware.tmforum.productcatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.DatasetId;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.mapping.annotations.RelationshipObject;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URL;
import java.util.List;

@MappingEnabled(entityType = ProductOffering.TYPE_PRODUCT_OFFERING)
public class BundleProductOffering implements ReferencedEntity {

    @Getter(onMethod = @__({@RelationshipObject, @DatasetId}))
    final URI id;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URI href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus")}))
    private String lifecycleStatus;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "bundledProductOfferingOption")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "bundledProductOfferingOption")}))
    private BundleProductOfferingOption bundledProductOfferingOption;

    /**
     * When sub-classing, this defines the super-class
     */
    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "@baseType")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "@baseType")}))
    @Nullable
    private String atBaseType;

    /**
     * A URI to a JSON-Schema file that defines additional attributes and relationships
     */
    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "@schemaLocation")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "@schemaLocation")}))
    @Nullable
    private URI atSchemaLocation;

    /**
     * When sub-classing, this defines the sub-class entity name.
     * We cannot use @type, since it clashes with the ngsi-ld type field(e.g. reserved name)
     */
    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "tmForumType")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "tmForumType")}))
    @Nullable
    private String atType;


    public BundleProductOffering(String id) {
        this.id = URI.create(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(ProductOffering.TYPE_PRODUCT_OFFERING);
    }
}
