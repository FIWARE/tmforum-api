package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCatalogChangeEventPayloadVO.*;

public class ServiceCatalogChangeEventPayloadVOTestExample {

	public static ServiceCatalogChangeEventPayloadVO build() {
		ServiceCatalogChangeEventPayloadVO exampleInstance = new ServiceCatalogChangeEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceCatalog(null);
		exampleInstance.setServiceCatalog(ServiceCatalogVOTestExample.build());
		return exampleInstance;
	}
}
