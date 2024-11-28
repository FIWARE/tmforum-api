package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.context.ServerRequestContext;

import java.io.IOException;
import java.util.Map;

public class PreservingDeserializer extends DelegatingDeserializer {
    private final BeanDescription beanDescription;
    private final EntityExtender entityExtender;

    public PreservingDeserializer(JsonDeserializer<?> d, BeanDescription beanDescription, EntityExtender entityExtender) {
        super(d);
        this.beanDescription = beanDescription;
        this.entityExtender = entityExtender;
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new PreservingDeserializer(newDelegatee, beanDescription, entityExtender);

    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        PropertyName schemaLocationProperty = new PropertyName("@schemaLocation");
        if (beanDescription.findProperties().stream().noneMatch(bpd -> bpd.hasName(schemaLocationProperty))) {
            return super.deserialize(p, ctxt);
        }
        Map plainValue = ctxt.readValue(p, Map.class);
        return entityExtender.addAndDeserializeVO(ServerRequestContext.currentRequest().hashCode(), plainValue, beanDescription.getBeanClass());
    }
}
