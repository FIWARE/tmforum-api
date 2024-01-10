package org.fiware.tmforum.partyrole;

import javax.inject.Singleton;

import org.fiware.partyRole.model.AccountRefVO;
import org.fiware.partyRole.model.AgreementRefVO;
import org.fiware.partyRole.model.CreditProfileVO;
import org.fiware.partyRole.model.PartyRoleVO;
import org.fiware.partyRole.model.PaymentMethodRefVO;
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
			fieldParamModule.addSerializer(PartyRoleVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AccountRefVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(CreditProfileVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AgreementRefVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(PaymentMethodRefVO.class, new FieldCleaningSerializer<>());
			
			objectMapper.registerModule(fieldParamModule);
			return objectMapper;
		}
	}
}

