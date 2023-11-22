package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCatalogChangeEventVO.*;

public class ResourceCatalogChangeEventVOTestExample {

	public static ResourceCatalogChangeEventVO build() {
		ResourceCatalogChangeEventVO exampleInstance = new ResourceCatalogChangeEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceCatalogChangeEventPayloadVOTestExample.build());
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
