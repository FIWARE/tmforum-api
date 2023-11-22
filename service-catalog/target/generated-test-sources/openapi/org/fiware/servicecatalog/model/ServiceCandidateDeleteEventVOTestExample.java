package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCandidateDeleteEventVO.*;

public class ServiceCandidateDeleteEventVOTestExample {

	public static ServiceCandidateDeleteEventVO build() {
		ServiceCandidateDeleteEventVO exampleInstance = new ServiceCandidateDeleteEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceCandidateDeleteEventPayloadVOTestExample.build());
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
