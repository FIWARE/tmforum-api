package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import lombok.RequiredArgsConstructor;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
public class ObjectMapperEventListener implements BeanCreatedEventListener<ObjectMapper> {

    @Override
    public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
        final ObjectMapper objectMapper = event.getBean();
        // overwrites the NON_EMPTY default, that breaks empty-list handling
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }
}
