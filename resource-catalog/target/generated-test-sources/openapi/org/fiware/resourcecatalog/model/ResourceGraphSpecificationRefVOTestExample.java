package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceGraphSpecificationRefVO.*;

public class ResourceGraphSpecificationRefVOTestExample {

	public static ResourceGraphSpecificationRefVO build() {
		ResourceGraphSpecificationRefVO exampleInstance = new ResourceGraphSpecificationRefVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setName("string");
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtReferredType("string");
		return exampleInstance;
	}
}
