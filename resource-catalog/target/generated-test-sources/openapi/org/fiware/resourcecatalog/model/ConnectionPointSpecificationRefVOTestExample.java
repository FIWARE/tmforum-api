package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ConnectionPointSpecificationRefVO.*;

public class ConnectionPointSpecificationRefVOTestExample {

	public static ConnectionPointSpecificationRefVO build() {
		ConnectionPointSpecificationRefVO exampleInstance = new ConnectionPointSpecificationRefVO();
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
