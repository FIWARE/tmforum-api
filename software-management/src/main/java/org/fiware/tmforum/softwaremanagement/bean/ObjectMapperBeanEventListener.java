package org.fiware.tmforum.softwaremanagement.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.softwaremanagement.model.*;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class ObjectMapperBeanEventListener implements BeanCreatedEventListener<ObjectMapper> {

    @Override
    public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
        log.debug("Add FieldCleaningSerializer to Software Management VOs");
        final ObjectMapper objectMapper = event.getBean();
        SimpleModule fieldParamModule = new SimpleModule();
        // we need to register per class, in order to use the generic serializer
        fieldParamModule.addSerializer(ResourceVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(SoftwareResourceVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ResourceGraphVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(PhysicalResourceVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(PhysicalResourceSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(LogicalResourceVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(LogicalResourceSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(InstalledSoftwareVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(HostingPlatformRequirementVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(HostingPlatformRequirementSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ConnectionVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ConnectionSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(APIVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(APISpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ResourceGraphSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ResourceSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(SoftwareResourceSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(SoftwareSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(SoftwareSupportPackageSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(SoftwareSupportPackageVO.class, new FieldCleaningSerializer<>());
        objectMapper.registerModule(fieldParamModule);
        return objectMapper;
    }
}