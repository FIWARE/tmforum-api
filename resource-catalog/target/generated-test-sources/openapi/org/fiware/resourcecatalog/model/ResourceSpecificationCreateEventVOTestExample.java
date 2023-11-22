package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceSpecificationCreateEventVO.*;

public class ResourceSpecificationCreateEventVOTestExample {

	public static ResourceSpecificationCreateEventVO build() {
		ResourceSpecificationCreateEventVO exampleInstance = new ResourceSpecificationCreateEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceSpecificationCreateEventPayloadVOTestExample.build());
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
