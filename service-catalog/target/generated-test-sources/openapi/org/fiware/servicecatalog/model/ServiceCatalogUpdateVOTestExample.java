package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCatalogUpdateVO.*;

public class ServiceCatalogUpdateVOTestExample {

	public static ServiceCatalogUpdateVO build() {
		ServiceCatalogUpdateVO exampleInstance = new ServiceCatalogUpdateVO();
		//initialize fields
		exampleInstance.setDescription("string");
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
