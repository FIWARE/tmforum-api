package org.fiware.tmforum.resourcefunction.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.net.URI;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Monitor.TYPE_MONITOR)
public class Monitor extends EntityWithId {

    public static final String TYPE_MONITOR = "monitor";

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URI href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "sourceHref")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "sourceHref")}))
    private URI sourceHref;

    // TODO: according to the doc it might be an enum: InProgress, InError, Completed. Needs clarification.
    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "state")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "state")}))
    private String state;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "request")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "request")}))
    private Request request;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "response")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "response")}))
    private Response response;

    public Monitor(String id) {
        super(TYPE_MONITOR, id);
    }

    @Override
    public String getEntityState() {
        return state;
    }
}
