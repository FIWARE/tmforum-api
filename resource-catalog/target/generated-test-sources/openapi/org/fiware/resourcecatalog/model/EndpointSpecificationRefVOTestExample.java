package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.EndpointSpecificationRefVO.*;

public class EndpointSpecificationRefVOTestExample {

	public static EndpointSpecificationRefVO build() {
		EndpointSpecificationRefVO exampleInstance = new EndpointSpecificationRefVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setIsRoot(true);
		exampleInstance.setName("string");
		exampleInstance.setRole("string");
		exampleInstance.setConnectionPointSpecification(null);
		exampleInstance.setConnectionPointSpecification(ConnectionPointSpecificationRefVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtReferredType("string");
		return exampleInstance;
	}
}
