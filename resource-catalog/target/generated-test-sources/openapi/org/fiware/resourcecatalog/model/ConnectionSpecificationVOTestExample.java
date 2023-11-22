package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ConnectionSpecificationVO.*;

public class ConnectionSpecificationVOTestExample {

	public static ConnectionSpecificationVO build() {
		ConnectionSpecificationVO exampleInstance = new ConnectionSpecificationVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setAssociationType("string");
		exampleInstance.setName("string");
		exampleInstance.setEndpointSpecification(java.util.List.of());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
