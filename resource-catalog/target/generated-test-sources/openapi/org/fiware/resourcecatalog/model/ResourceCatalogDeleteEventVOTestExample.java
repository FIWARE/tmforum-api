package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCatalogDeleteEventVO.*;

public class ResourceCatalogDeleteEventVOTestExample {

	public static ResourceCatalogDeleteEventVO build() {
		ResourceCatalogDeleteEventVO exampleInstance = new ResourceCatalogDeleteEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ResourceCatalogDeleteEventPayloadVOTestExample.build());
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
