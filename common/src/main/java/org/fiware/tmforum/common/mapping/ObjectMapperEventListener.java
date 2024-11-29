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

	@Override
	public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
		final ObjectMapper objectMapper = event.getBean();
		// overwrites the NON_EMPTY default, that breaks empty-list handling
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		SimpleModule deserializerModule = new SimpleModule();
		// inject the schema validator for atSchemaLocation handling
		deserializerModule.setDeserializerModifier(new BeanDeserializerModifier() {
			@Override
			public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
														  BeanDescription beanDescription,
														  JsonDeserializer<?> originalDeserializer) {
				return new ValidatingDeserializer(originalDeserializer, beanDescription);
			}
		});
		objectMapper.registerModule(deserializerModule);
		return objectMapper;
	}
}
