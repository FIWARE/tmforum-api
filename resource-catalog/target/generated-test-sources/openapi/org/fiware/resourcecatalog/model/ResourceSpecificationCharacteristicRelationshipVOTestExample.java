package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceSpecificationCharacteristicRelationshipVO.*;

public class ResourceSpecificationCharacteristicRelationshipVOTestExample {

	public static ResourceSpecificationCharacteristicRelationshipVO build() {
		ResourceSpecificationCharacteristicRelationshipVO exampleInstance = new ResourceSpecificationCharacteristicRelationshipVO();
		//initialize fields
		exampleInstance.setCharacteristicSpecificationId("string");
		exampleInstance.setName("string");
		exampleInstance.setRelationshipType("string");
		exampleInstance.setResourceSpecificationHref(java.net.URI.create("my:uri"));
		exampleInstance.setResourceSpecificationId("string");
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
