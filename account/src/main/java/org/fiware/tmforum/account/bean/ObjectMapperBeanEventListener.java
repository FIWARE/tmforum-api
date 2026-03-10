package org.fiware.tmforum.account.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.account.model.BillFormatVO;
import org.fiware.account.model.BillPresentationMediaVO;
import org.fiware.account.model.BillStructureVO;
import org.fiware.account.model.BillingAccountVO;
import org.fiware.account.model.BillingCycleSpecificationVO;
import org.fiware.account.model.FinancialAccountVO;
import org.fiware.account.model.PartyAccountVO;
import org.fiware.account.model.SettlementAccountVO;
import org.fiware.tmforum.common.mapping.FieldCleaningSerializer;

import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor
@Slf4j
public class ObjectMapperBeanEventListener implements BeanCreatedEventListener<ObjectMapper> {

    @Override
    public ObjectMapper onCreated(BeanCreatedEvent<ObjectMapper> event) {
        log.debug("Add FieldCleaningSerializer to Account VOs");
        final ObjectMapper objectMapper = event.getBean();
        SimpleModule fieldParamModule = new SimpleModule();
        // we need to register per class, in order to use the generic serializer
        fieldParamModule.addSerializer(BillFormatVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(BillingAccountVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(BillingCycleSpecificationVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(BillPresentationMediaVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(FinancialAccountVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(PartyAccountVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(SettlementAccountVO.class, new FieldCleaningSerializer<>());
        fieldParamModule.addSerializer(BillStructureVO.class, new FieldCleaningSerializer<>());
        //fieldParamModule.addSerializer(BillingCycleSpecificationRefOrValueVO.class, new FieldCleaningSerializer<>());
        objectMapper.registerModule(fieldParamModule);
        return objectMapper;
    }
}