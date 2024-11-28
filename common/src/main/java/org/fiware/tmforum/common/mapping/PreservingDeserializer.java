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
    private final ApplicationContext applicationContext;

    public PreservingDeserializer(JsonDeserializer<?> d, BeanDescription beanDescription, ApplicationContext applicationContext) {
        super(d);
        this.beanDescription = beanDescription;
        this.applicationContext = applicationContext;
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new PreservingDeserializer(newDelegatee, beanDescription, applicationContext);

    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        PropertyName schemaLocationProperty = new PropertyName("@schemaLocation");
        if (beanDescription.findProperties().stream().noneMatch(bpd -> bpd.hasName(schemaLocationProperty))) {
            return super.deserialize(p, ctxt);
        }
        Map plainValue = ctxt.readValue(p, Map.class);
        return applicationContext
                .getBean(EntityExtender.class)
                .addAndDeserializeVO(ServerRequestContext.currentRequest().hashCode(), plainValue, beanDescription.getBeanClass());
    }
}
