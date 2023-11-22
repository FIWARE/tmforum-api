package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCatalogDeleteEventVO.*;

public class ServiceCatalogDeleteEventVOTestExample {

	public static ServiceCatalogDeleteEventVO build() {
		ServiceCatalogDeleteEventVO exampleInstance = new ServiceCatalogDeleteEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceCatalogDeleteEventPayloadVOTestExample.build());
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
