package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceSpecificationCreateEventVO.*;

public class ServiceSpecificationCreateEventVOTestExample {

	public static ServiceSpecificationCreateEventVO build() {
		ServiceSpecificationCreateEventVO exampleInstance = new ServiceSpecificationCreateEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceSpecificationCreateEventPayloadVOTestExample.build());
		exampleInstance.setEventId("string");
		exampleInstance.setEventTime(null);
		exampleInstance.setEventType("string");
		exampleInstance.setCorrelationId("string");
		exampleInstance.setDomain("string");
		exampleInstance.setTitle("string");
		exampleInstance.setDescription("string");
		exampleInstance.setPriority("string");
		exampleInstance.setTimeOcurred(null);
		return exampleInstance;
	}
}
