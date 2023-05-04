package org.fiware.tmforum.agreement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.runtime.Micronaut;
import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;
import org.fiware.agreement.model.AgreementAuthorizationVO;
import org.fiware.agreement.model.AgreementItemVO;
import org.fiware.agreement.model.AgreementSpecCharacteristicVO;
import org.fiware.agreement.model.AgreementSpecCharacteristicValueVO;
import org.fiware.agreement.model.AgreementSpecificationRefVO;
import org.fiware.agreement.model.AgreementSpecificationRelationshipVO;
import org.fiware.agreement.model.AgreementSpecificationVO;
import org.fiware.agreement.model.AgreementTermOrConditionVO;
import org.fiware.agreement.model.AgreementVO;
import org.fiware.agreement.model.CategoryRefVO;

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
			fieldParamModule.addSerializer(AgreementVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AgreementSpecificationVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AgreementAuthorizationVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AgreementItemVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AgreementSpecCharacteristicVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AgreementSpecCharacteristicValueVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AgreementSpecificationRefVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AgreementSpecificationRelationshipVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AgreementTermOrConditionVO.class, new FieldCleaningSerializer<>());

			objectMapper.registerModule(fieldParamModule);
			return objectMapper;
		}
	}
}
