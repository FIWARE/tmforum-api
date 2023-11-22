package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCategoryDeleteEventVO.*;

public class ServiceCategoryDeleteEventVOTestExample {

	public static ServiceCategoryDeleteEventVO build() {
		ServiceCategoryDeleteEventVO exampleInstance = new ServiceCategoryDeleteEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceCategoryDeleteEventPayloadVOTestExample.build());
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
