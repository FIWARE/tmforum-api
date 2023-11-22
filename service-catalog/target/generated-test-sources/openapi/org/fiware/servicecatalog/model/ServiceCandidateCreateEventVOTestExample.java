package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCandidateCreateEventVO.*;

public class ServiceCandidateCreateEventVOTestExample {

	public static ServiceCandidateCreateEventVO build() {
		ServiceCandidateCreateEventVO exampleInstance = new ServiceCandidateCreateEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceCandidateCreateEventPayloadVOTestExample.build());
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
