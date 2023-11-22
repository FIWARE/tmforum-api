package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCatalogCreateEventVO.*;

public class ResourceCatalogCreateEventVOTestExample {

	public static ResourceCatalogCreateEventVO build() {
		ResourceCatalogCreateEventVO exampleInstance = new ResourceCatalogCreateEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceCatalogCreateEventPayloadVOTestExample.build());
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
