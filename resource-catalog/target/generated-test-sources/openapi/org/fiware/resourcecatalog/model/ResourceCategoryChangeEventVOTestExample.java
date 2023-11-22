package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCategoryChangeEventVO.*;

public class ResourceCategoryChangeEventVOTestExample {

	public static ResourceCategoryChangeEventVO build() {
		ResourceCategoryChangeEventVO exampleInstance = new ResourceCategoryChangeEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceCategoryChangeEventPayloadVOTestExample.build());
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
