package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCategoryRefVO.*;

public class ResourceCategoryRefVOTestExample {

	public static ResourceCategoryRefVO build() {
		ResourceCategoryRefVO exampleInstance = new ResourceCategoryRefVO();
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
