package org.fiware.tmforum.common.domain.subscription;

import io.github.wistefan.mapping.annotations.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.mapping.IdHelper;

import java.net.URI;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode
@MappingEnabled(subscriptionType = Subscription.TYPE_SUBSCRIPTION)
public class Subscription {
    public static final String TYPE_SUBSCRIPTION = "Subscription";

    /**
     * Type of the subscription
     */
    @Getter(onMethod = @__({ @SubscriptionType}))
    private final String type;

    /**
     * ID of the subscription. This is the id part of "urn:ngsi-ld:TYPE:ID"
     */
    @Ignore
    @Getter(onMethod = @__({ @SubscriptionId }))
    @Setter
    private URI id;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "q")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "q")}))
    private String q;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "notification")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "notification",
            targetClass = NotificationParams.class)}))
    private NotificationParams notification;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "entities")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "entities",
            targetClass = EntityInfo.class)}))
    private List<EntityInfo> entities;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description")}))
    private String description;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "watchedAttributes")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "watchedAttributes")}))
    private Set<String> watchedAttributes;

    public Subscription(String id) {
        this.type = TYPE_SUBSCRIPTION;
        if (IdHelper.isNgsiLdId(id)) {
            this.id = URI.create(id);
        } else {
            this.id = IdHelper.toNgsiLd(id, TYPE_SUBSCRIPTION);
        }
    }

    public Subscription() {
        this.type = TYPE_SUBSCRIPTION;
    }
}
