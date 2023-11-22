package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceGraphSpecificationRelationshipVO.*;

public class ResourceGraphSpecificationRelationshipVOTestExample {

	public static ResourceGraphSpecificationRelationshipVO build() {
		ResourceGraphSpecificationRelationshipVO exampleInstance = new ResourceGraphSpecificationRelationshipVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setRelationshipType("string");
		exampleInstance.setResourceGraph(null);
		exampleInstance.setResourceGraph(ResourceGraphSpecificationRefVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
