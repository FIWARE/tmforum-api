package org.fiware.tmforum.servicecatalog.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.servicecatalog.model.ServiceCandidateVO;
import org.fiware.servicecatalog.model.ServiceCatalogVO;
import org.fiware.servicecatalog.model.ServiceCategoryVO;
import org.fiware.servicecatalog.model.ServiceSpecificationVO;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class ObjectMapperBeanEventListener implements BeanCreatedEventListener<ObjectMapper> {

    @Override
    public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
        log.debug("Add FieldCleaningSerializer to ServiceCatalog VOs");
        final ObjectMapper objectMapper = event.getBean();
        SimpleModule fieldParamModule = new SimpleModule();
        // we need to register per class, in order to use the generic serializer
        fieldParamModule.addSerializer(ServiceCandidateVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ServiceCatalogVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ServiceCategoryVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ServiceSpecificationVO.class, new FieldCleaningSerializer<>());
        objectMapper.registerModule(fieldParamModule);
        return objectMapper;
    }
}