package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.wistefan.mapping.AdditionalPropertyMixin;
import io.github.wistefan.mapping.CacheSerdeableObjectMapper;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.model.*;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
public class ObjectMapperEventListener implements BeanCreatedEventListener<ObjectMapper> {

	@Override
	public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
		final ObjectMapper objectMapper = event.getBean();
		// overwrites the NON_EMPTY default, that breaks empty-list handling
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		objectMapper.addMixIn(PropertyListVO.class, ListVOMixin.class);
		objectMapper.addMixIn(RelationshipListVO.class, ListVOMixin.class);
		objectMapper.addMixIn(GeoPropertyListVO.class, ListVOMixin.class);
		// when using caches, a specific mapper is required to serialize/desiralize the cache entries.
		// we do not want and do not need to overwrite its already configured Mixin's
		if (!(objectMapper instanceof CacheSerdeableObjectMapper)) {
			objectMapper.addMixIn(AdditionalPropertyVO.class, AdditionalPropertyMixin.class);
		}

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
