package org.fiware.tmforum.agreement.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.agreement.model.AgreementAuthorizationVO;
import org.fiware.agreement.model.AgreementItemVO;
import org.fiware.agreement.model.AgreementSpecCharacteristicVO;
import org.fiware.agreement.model.AgreementSpecCharacteristicValueVO;
import org.fiware.agreement.model.AgreementSpecificationRefVO;
import org.fiware.agreement.model.AgreementSpecificationRelationshipVO;
import org.fiware.agreement.model.AgreementSpecificationVO;
import org.fiware.agreement.model.AgreementTermOrConditionVO;
import org.fiware.agreement.model.AgreementVO;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class ObjectMapperBeanEventListener implements BeanCreatedEventListener<ObjectMapper> {

    @Override
    public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
        log.debug("Add FieldCleaningSerializer to Agreement VOs");
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