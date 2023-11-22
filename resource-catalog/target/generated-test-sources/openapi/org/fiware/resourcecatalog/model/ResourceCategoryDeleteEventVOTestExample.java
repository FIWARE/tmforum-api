package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCategoryDeleteEventVO.*;

public class ResourceCategoryDeleteEventVOTestExample {

	public static ResourceCategoryDeleteEventVO build() {
		ResourceCategoryDeleteEventVO exampleInstance = new ResourceCategoryDeleteEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceCategoryDeleteEventPayloadVOTestExample.build());
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
