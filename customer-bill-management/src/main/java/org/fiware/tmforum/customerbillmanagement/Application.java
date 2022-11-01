package org.fiware.tmforum.customerbillmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.runtime.Micronaut;
import lombok.RequiredArgsConstructor;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateVO;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandVO;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBill;

import javax.inject.Singleton;
import java.time.Clock;

/**
 * Base application as starting point
 */
@Factory
public class Application {

	public static void main(String[] args) {
		Micronaut.run(Application.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Singleton
	@RequiredArgsConstructor
	static class ObjectMapperBeanEventListener implements BeanCreatedEventListener<ObjectMapper> {

		@Override
		public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {

			final ObjectMapper objectMapper = event.getBean();
			SimpleModule fieldParamModule = new SimpleModule();
			// we need to register per class, in order to use the generic serializer
			fieldParamModule.addSerializer(CustomerBillVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(CustomerBillOnDemandVO.class, new FieldCleaningSerializer<>());
			fieldParamModule.addSerializer(AppliedCustomerBillingRateVO.class, new FieldCleaningSerializer<>());
			objectMapper.registerModule(fieldParamModule);
			return objectMapper;
		}
	}

}
