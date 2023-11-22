package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCatalogBatchEventVO.*;

public class ServiceCatalogBatchEventVOTestExample {

	public static ServiceCatalogBatchEventVO build() {
		ServiceCatalogBatchEventVO exampleInstance = new ServiceCatalogBatchEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ServiceCatalogBatchEventPayloadVOTestExample.build());
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
