package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCatalogCreateVO.*;

public class ServiceCatalogCreateVOTestExample {

	public static ServiceCatalogCreateVO build() {
		ServiceCatalogCreateVO exampleInstance = new ServiceCatalogCreateVO();
		//initialize fields
		exampleInstance.setDescription("string");
		exampleInstance.setLastUpdate(null);
		exampleInstance.setLifecycleStatus("string");
		exampleInstance.setName("string");
		exampleInstance.setVersion("string");
		exampleInstance.setCategory(java.util.List.of());
		exampleInstance.setRelatedParty(java.util.List.of());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
