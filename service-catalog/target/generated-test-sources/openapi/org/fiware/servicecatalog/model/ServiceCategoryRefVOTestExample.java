package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCategoryRefVO.*;

public class ServiceCategoryRefVOTestExample {

	public static ServiceCategoryRefVO build() {
		ServiceCategoryRefVO exampleInstance = new ServiceCategoryRefVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setName("string");
		exampleInstance.setVersion("string");
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtReferredType("string");
		return exampleInstance;
	}
}
