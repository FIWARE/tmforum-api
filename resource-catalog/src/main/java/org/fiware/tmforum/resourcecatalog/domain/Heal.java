package org.fiware.tmforum.resourcecatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.List;


@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Heal.TYPE_HEAL)
public class Heal extends EntityWithId {

    public static final String TYPE_HEAL = "heal";

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URI href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "cause")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "cause")}))
    private String cause;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "degreeOfHealing")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "degreeOfHealing")}))
    private String degreeOfHealing;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "healAction")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "healAction")}))
    private String healAction;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    // Defined as string by the api, no format restrictions applied. Check if that changes in the future.
    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "startTime")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "startTime")}))
    private String startTime;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "additionalParms")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "additionalParms", targetClass = Characteristic.class)}))
    private List<Characteristic> additionalParms;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "healPolicy")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "healPolicy", targetClass = HealPolicyRef.class)}))
    private HealPolicyRef healPolicy;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "resourceFunction")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "resourceFunction", targetClass = ResourceFunctionRef.class)}))
    private ResourceFunctionRef resourceFunction;

    protected Heal(String id) {
        super(TYPE_HEAL, id);
    }
}
