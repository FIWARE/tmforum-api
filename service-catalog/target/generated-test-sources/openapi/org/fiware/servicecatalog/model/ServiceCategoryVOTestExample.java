package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCategoryVO.*;

public class ServiceCategoryVOTestExample {

	public static ServiceCategoryVO build() {
		ServiceCategoryVO exampleInstance = new ServiceCategoryVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setDescription("string");
		exampleInstance.setIsRoot(false);
		exampleInstance.setLastUpdate(null);
		exampleInstance.setLifecycleStatus("string");
		exampleInstance.setName("string");
		exampleInstance.setParentId("string");
		exampleInstance.setVersion("string");
		exampleInstance.setCategory(java.util.List.of());
		exampleInstance.setServiceCandidate(java.util.List.of());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
