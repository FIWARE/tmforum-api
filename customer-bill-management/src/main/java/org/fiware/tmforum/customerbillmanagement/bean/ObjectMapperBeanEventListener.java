package org.fiware.tmforum.customerbillmanagement.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateVO;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandVO;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class ObjectMapperBeanEventListener implements BeanCreatedEventListener<ObjectMapper> {

    @Override
    public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
        log.debug("Add FieldCleaningSerializer to CustomBill VOs");
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