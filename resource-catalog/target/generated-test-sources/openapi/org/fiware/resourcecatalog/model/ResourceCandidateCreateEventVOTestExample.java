package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCandidateCreateEventVO.*;

public class ResourceCandidateCreateEventVOTestExample {

	public static ResourceCandidateCreateEventVO build() {
		ResourceCandidateCreateEventVO exampleInstance = new ResourceCandidateCreateEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceCandidateCreateEventPayloadVOTestExample.build());
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
