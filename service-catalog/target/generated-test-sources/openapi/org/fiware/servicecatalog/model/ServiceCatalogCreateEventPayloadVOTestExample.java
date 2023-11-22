package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCatalogCreateEventPayloadVO.*;

public class ServiceCatalogCreateEventPayloadVOTestExample {

	public static ServiceCatalogCreateEventPayloadVO build() {
		ServiceCatalogCreateEventPayloadVO exampleInstance = new ServiceCatalogCreateEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceCatalog(null);
		exampleInstance.setServiceCatalog(ServiceCatalogVOTestExample.build());
		return exampleInstance;
	}
}
