package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCatalogChangeEventVO.*;

public class ServiceCatalogChangeEventVOTestExample {

	public static ServiceCatalogChangeEventVO build() {
		ServiceCatalogChangeEventVO exampleInstance = new ServiceCatalogChangeEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceCatalogChangeEventPayloadVOTestExample.build());
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
