package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCategoryCreateEventVO.*;

public class ResourceCategoryCreateEventVOTestExample {

	public static ResourceCategoryCreateEventVO build() {
		ResourceCategoryCreateEventVO exampleInstance = new ResourceCategoryCreateEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceCategoryCreateEventPayloadVOTestExample.build());
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
