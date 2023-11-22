package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCandidateChangeEventVO.*;

public class ResourceCandidateChangeEventVOTestExample {

	public static ResourceCandidateChangeEventVO build() {
		ResourceCandidateChangeEventVO exampleInstance = new ResourceCandidateChangeEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceCandidateChangeEventPayloadVOTestExample.build());
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
