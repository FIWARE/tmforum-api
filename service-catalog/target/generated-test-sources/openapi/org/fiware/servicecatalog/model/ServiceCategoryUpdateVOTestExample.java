package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCategoryUpdateVO.*;

public class ServiceCategoryUpdateVOTestExample {

	public static ServiceCategoryUpdateVO build() {
		ServiceCategoryUpdateVO exampleInstance = new ServiceCategoryUpdateVO();
		//initialize fields
		exampleInstance.setDescription("string");
		exampleInstance.setIsRoot(false);
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
