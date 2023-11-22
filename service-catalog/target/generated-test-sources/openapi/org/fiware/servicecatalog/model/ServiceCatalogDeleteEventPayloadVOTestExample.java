package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCatalogDeleteEventPayloadVO.*;

public class ServiceCatalogDeleteEventPayloadVOTestExample {

	public static ServiceCatalogDeleteEventPayloadVO build() {
		ServiceCatalogDeleteEventPayloadVO exampleInstance = new ServiceCatalogDeleteEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceCatalog(null);
		exampleInstance.setServiceCatalog(ServiceCatalogVOTestExample.build());
		return exampleInstance;
	}
}
