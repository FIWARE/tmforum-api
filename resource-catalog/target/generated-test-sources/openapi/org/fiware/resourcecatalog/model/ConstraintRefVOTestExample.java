package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ConstraintRefVO.*;

public class ConstraintRefVOTestExample {

	public static ConstraintRefVO build() {
		ConstraintRefVO exampleInstance = new ConstraintRefVO();
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
