package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceGraphSpecificationVO.*;

public class ResourceGraphSpecificationVOTestExample {

	public static ResourceGraphSpecificationVO build() {
		ResourceGraphSpecificationVO exampleInstance = new ResourceGraphSpecificationVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setDescription("string");
		exampleInstance.setName("string");
		exampleInstance.setConnectionSpecification(java.util.List.of());
		exampleInstance.setGraphSpecificationRelationship(java.util.List.of());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
