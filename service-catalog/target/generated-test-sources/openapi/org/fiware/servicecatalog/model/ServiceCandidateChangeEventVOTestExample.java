package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCandidateChangeEventVO.*;

public class ServiceCandidateChangeEventVOTestExample {

	public static ServiceCandidateChangeEventVO build() {
		ServiceCandidateChangeEventVO exampleInstance = new ServiceCandidateChangeEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceCandidateChangeEventPayloadVOTestExample.build());
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
