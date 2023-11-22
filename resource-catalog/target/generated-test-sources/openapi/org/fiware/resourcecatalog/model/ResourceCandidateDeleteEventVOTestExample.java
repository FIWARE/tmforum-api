package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCandidateDeleteEventVO.*;

public class ResourceCandidateDeleteEventVOTestExample {

	public static ResourceCandidateDeleteEventVO build() {
		ResourceCandidateDeleteEventVO exampleInstance = new ResourceCandidateDeleteEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceCandidateDeleteEventPayloadVOTestExample.build());
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
