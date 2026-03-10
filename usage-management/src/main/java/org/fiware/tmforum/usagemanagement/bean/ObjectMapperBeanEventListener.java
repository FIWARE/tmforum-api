package org.fiware.tmforum.usagemanagement.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;
import org.fiware.usagemanagement.model.RatedProductUsageVO;
import org.fiware.usagemanagement.model.UsageCharacteristicVO;
import org.fiware.usagemanagement.model.UsageSpecificationVO;
import org.fiware.usagemanagement.model.UsageStatusTypeVO;
import org.fiware.usagemanagement.model.UsageVO;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class ObjectMapperBeanEventListener implements BeanCreatedEventListener<ObjectMapper> {

    @Override
    public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
        log.debug("Add FieldCleaningSerializer to Usage VOs");
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