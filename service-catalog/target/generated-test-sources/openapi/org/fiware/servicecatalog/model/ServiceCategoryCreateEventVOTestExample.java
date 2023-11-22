package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCategoryCreateEventVO.*;

public class ServiceCategoryCreateEventVOTestExample {

	public static ServiceCategoryCreateEventVO build() {
		ServiceCategoryCreateEventVO exampleInstance = new ServiceCategoryCreateEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceCategoryCreateEventPayloadVOTestExample.build());
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
