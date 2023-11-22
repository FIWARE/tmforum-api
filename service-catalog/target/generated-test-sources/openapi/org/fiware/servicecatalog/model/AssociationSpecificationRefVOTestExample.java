package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.AssociationSpecificationRefVO.*;

public class AssociationSpecificationRefVOTestExample {

	public static AssociationSpecificationRefVO build() {
		AssociationSpecificationRefVO exampleInstance = new AssociationSpecificationRefVO();
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
