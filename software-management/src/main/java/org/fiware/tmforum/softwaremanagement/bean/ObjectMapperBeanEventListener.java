package org.fiware.tmforum.softwaremanagement.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.softwaremanagement.model.APISpecificationVO;
import org.fiware.softwaremanagement.model.APIVO;
import org.fiware.softwaremanagement.model.ConnectionSpecificationVO;
import org.fiware.softwaremanagement.model.ConnectionVO;
import org.fiware.softwaremanagement.model.HostingPlatformRequirementSpecificationVO;
import org.fiware.softwaremanagement.model.HostingPlatformRequirementVO;
import org.fiware.softwaremanagement.model.InstalledSoftwareVO;
import org.fiware.softwaremanagement.model.LogicalResourceSpecificationVO;
import org.fiware.softwaremanagement.model.LogicalResourceVO;
import org.fiware.softwaremanagement.model.PhysicalResourceSpecificationVO;
import org.fiware.softwaremanagement.model.PhysicalResourceVO;
import org.fiware.softwaremanagement.model.ResourceGraphSpecificationVO;
import org.fiware.softwaremanagement.model.ResourceGraphVO;
import org.fiware.softwaremanagement.model.ResourceSpecificationVO;
import org.fiware.softwaremanagement.model.ResourceVO;
import org.fiware.softwaremanagement.model.SoftwareResourceSpecificationVO;
import org.fiware.softwaremanagement.model.SoftwareResourceVO;
import org.fiware.softwaremanagement.model.SoftwareSpecificationVO;
import org.fiware.softwaremanagement.model.SoftwareSupportPackageSpecificationVO;
import org.fiware.softwaremanagement.model.SoftwareSupportPackageVO;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;

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