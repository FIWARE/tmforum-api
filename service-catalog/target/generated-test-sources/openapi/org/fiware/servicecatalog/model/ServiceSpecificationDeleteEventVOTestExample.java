package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceSpecificationDeleteEventVO.*;

public class ServiceSpecificationDeleteEventVOTestExample {

	public static ServiceSpecificationDeleteEventVO build() {
		ServiceSpecificationDeleteEventVO exampleInstance = new ServiceSpecificationDeleteEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceSpecificationDeleteEventPayloadVOTestExample.build());
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
