package org.fiware.tmforum.usagemanagement;

import javax.inject.Singleton;

import org.fiware.usagemanagement.model.RatedProductUsageVO;
import org.fiware.usagemanagement.model.UsageVO;
import org.fiware.usagemanagement.model.UsageCharacteristicVO;
import org.fiware.usagemanagement.model.UsageSpecificationVO;
import org.fiware.usagemanagement.model.UsageStatusTypeVO;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.runtime.Micronaut;
import lombok.RequiredArgsConstructor;

/**
 * Base application as starting point
 */
@Factory
public class Application {


	/* Non está implementado */
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
			fieldParamModule.addSerializer(RatedProductUsageVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(UsageVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(UsageCharacteristicVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(UsageSpecificationVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(UsageStatusTypeVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(UsageStatusTypeVO.class, new FieldCleaningSerializer<>());

			objectMapper.registerModule(fieldParamModule);
			return objectMapper;
		}
	}
}
