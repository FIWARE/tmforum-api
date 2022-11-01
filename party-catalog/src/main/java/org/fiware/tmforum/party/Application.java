package org.fiware.tmforum.party;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.runtime.Micronaut;
import lombok.RequiredArgsConstructor;
import org.fiware.party.model.IndividualVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;

import javax.inject.Singleton;

/**
 * Base application as starting point
 */
@Factory
public class Application {

	public static void main(String[] args) {
		Micronaut.run(Application.class, args);
	}

	@Singleton
	@RequiredArgsConstructor
	static class ObjectMapperBeanEventListener implements BeanCreatedEventListener<ObjectMapper> {

		@Override
		public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {

			final ObjectMapper objectMapper = event.getBean();
			SimpleModule fieldParamModule = new SimpleModule();
			// we need to register per class, in order to use the generic serializer
			fieldParamModule.addSerializer(IndividualVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(OrganizationVO.class, new FieldCleaningSerializer<>());
			objectMapper.registerModule(fieldParamModule);
			return objectMapper;
		}
	}
}
