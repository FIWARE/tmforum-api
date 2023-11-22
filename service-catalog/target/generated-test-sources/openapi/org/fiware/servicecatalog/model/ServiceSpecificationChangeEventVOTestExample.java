package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceSpecificationChangeEventVO.*;

public class ServiceSpecificationChangeEventVOTestExample {

	public static ServiceSpecificationChangeEventVO build() {
		ServiceSpecificationChangeEventVO exampleInstance = new ServiceSpecificationChangeEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceSpecificationChangeEventPayloadVOTestExample.build());
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
