package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceSpecificationCharacteristicVO.*;

public class ResourceSpecificationCharacteristicVOTestExample {

	public static ResourceSpecificationCharacteristicVO build() {
		ResourceSpecificationCharacteristicVO exampleInstance = new ResourceSpecificationCharacteristicVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setConfigurable(false);
		exampleInstance.setDescription("string");
		exampleInstance.setExtensible(false);
		exampleInstance.setIsUnique(false);
		exampleInstance.setMaxCardinality(12);
		exampleInstance.setMinCardinality(12);
		exampleInstance.setName("string");
		exampleInstance.setRegex("string");
		exampleInstance.setValueType("string");
		exampleInstance.setResourceSpecCharRelationship(java.util.List.of());
		exampleInstance.setResourceSpecCharacteristicValue(java.util.List.of());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtValueSchemaLocation("string");
		return exampleInstance;
	}
}
