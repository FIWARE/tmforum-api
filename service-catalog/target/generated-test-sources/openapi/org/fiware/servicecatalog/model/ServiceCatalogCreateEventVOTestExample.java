package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCatalogCreateEventVO.*;

public class ServiceCatalogCreateEventVOTestExample {

	public static ServiceCatalogCreateEventVO build() {
		ServiceCatalogCreateEventVO exampleInstance = new ServiceCatalogCreateEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceCatalogCreateEventPayloadVOTestExample.build());
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
