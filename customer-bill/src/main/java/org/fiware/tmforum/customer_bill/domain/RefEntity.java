package org.fiware.tmforum.customer_bill.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.mapping.annotations.*;

import java.net.URI;

@EqualsAndHashCode
public abstract class RefEntity extends Entity implements ReferencedEntity {

    @Getter(onMethod = @__({@RelationshipObject, @DatasetId}))
    final URI id;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URI href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "@referredType")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "@referredType")}))
    private String atReferredType;

    protected RefEntity(String id) {
        this.id = URI.create(id);
    }

    protected RefEntity(URI id) {
        this.id = id;
    }

}
