package org.fiware.tmforum.resourcecatalog;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.runtime.Micronaut;
import lombok.RequiredArgsConstructor;
import org.fiware.resourcecatalog.model.ResourceCandidateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogVO;
import org.fiware.resourcecatalog.model.ResourceCategoryVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;

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
            fieldParamModule.addSerializer(ResourceSpecificationVO.class, new FieldCleaningSerializer<>());
            fieldParamModule.addSerializer(ResourceCategoryVO.class, new FieldCleaningSerializer<>());
            fieldParamModule.addSerializer(ResourceCatalogVO.class, new FieldCleaningSerializer<>());
            fieldParamModule.addSerializer(ResourceCandidateVO.class, new FieldCleaningSerializer<>());
            objectMapper.registerModule(fieldParamModule);
            return objectMapper;
        }
    }


}
