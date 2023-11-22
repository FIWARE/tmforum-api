package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceSpecificationDeleteEventVO.*;

public class ResourceSpecificationDeleteEventVOTestExample {

	public static ResourceSpecificationDeleteEventVO build() {
		ResourceSpecificationDeleteEventVO exampleInstance = new ResourceSpecificationDeleteEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceSpecificationDeleteEventPayloadVOTestExample.build());
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
