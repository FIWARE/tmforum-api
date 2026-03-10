package org.fiware.tmforum.resourcecatalog.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.resourcecatalog.model.ResourceCandidateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogVO;
import org.fiware.resourcecatalog.model.ResourceCategoryVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class ObjectMapperBeanEventListener implements BeanCreatedEventListener<ObjectMapper> {

    @Override
    public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
        log.debug("Add FieldCleaningSerializer to ResourceCatalog VOs");
        final ObjectMapper objectMapper = event.getBean();
        SimpleModule fieldParamModule = new SimpleModule();
        // we need to register per class, in order to use the generic serializer
        fieldParamModule.addSerializer(ResourceSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ResourceCategoryVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ResourceCatalogVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(ResourceCandidateVO.class, new FieldCleaningSerializer<>());
        objectMapper.registerModule(fieldParamModule);
        return objectMapper;
    }
}