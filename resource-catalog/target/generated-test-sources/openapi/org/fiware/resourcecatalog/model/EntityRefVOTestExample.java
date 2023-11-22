package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.EntityRefVO.*;

public class EntityRefVOTestExample {

	public static EntityRefVO build() {
		EntityRefVO exampleInstance = new EntityRefVO();
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
