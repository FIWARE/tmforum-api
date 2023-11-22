package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCatalogBatchEventPayloadVO.*;

public class ServiceCatalogBatchEventPayloadVOTestExample {

	public static ServiceCatalogBatchEventPayloadVO build() {
		ServiceCatalogBatchEventPayloadVO exampleInstance = new ServiceCatalogBatchEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceCatalog(null);
		exampleInstance.setServiceCatalog(ServiceCatalogVOTestExample.build());
		return exampleInstance;
	}
}
