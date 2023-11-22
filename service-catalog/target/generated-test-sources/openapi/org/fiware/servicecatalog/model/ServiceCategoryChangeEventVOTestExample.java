package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCategoryChangeEventVO.*;

public class ServiceCategoryChangeEventVOTestExample {

	public static ServiceCategoryChangeEventVO build() {
		ServiceCategoryChangeEventVO exampleInstance = new ServiceCategoryChangeEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceCategoryChangeEventPayloadVOTestExample.build());
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
