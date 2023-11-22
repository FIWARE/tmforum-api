package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceSpecificationRelationshipVO.*;

public class ResourceSpecificationRelationshipVOTestExample {

	public static ResourceSpecificationRelationshipVO build() {
		ResourceSpecificationRelationshipVO exampleInstance = new ResourceSpecificationRelationshipVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setDefaultQuantity(12);
		exampleInstance.setMaximumQuantity(12);
		exampleInstance.setMinimumQuantity(12);
		exampleInstance.setName("string");
		exampleInstance.setRelationshipType("string");
		exampleInstance.setRole("string");
		exampleInstance.setCharacteristic(java.util.List.of());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
