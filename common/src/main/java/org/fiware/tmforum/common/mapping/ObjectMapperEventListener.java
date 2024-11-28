package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import lombok.RequiredArgsConstructor;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
public class ObjectMapperEventListener implements BeanCreatedEventListener<ObjectMapper> {

    // we need to inject the application context here and then get the entity extender in the serializer,
    // since the extender requires the object mapper which would create a circular dependency
    private final ApplicationContext applicationContext;

    @Override
    public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
        final ObjectMapper objectMapper = event.getBean();
        // overwrites the NON_EMPTY default, that breaks empty-list handling
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        SimpleModule deserializerModule = new SimpleModule();
        deserializerModule.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                          BeanDescription beanDescription,
                                                          JsonDeserializer<?> originalDeserializer) {
                return new PreservingDeserializer(originalDeserializer, beanDescription, applicationContext);
            }
        });
        objectMapper.registerModule(deserializerModule);
        return objectMapper;
    }
}
