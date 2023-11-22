package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.CharacteristicSpecificationVO.*;

public class CharacteristicSpecificationVOTestExample {

	public static CharacteristicSpecificationVO build() {
		CharacteristicSpecificationVO exampleInstance = new CharacteristicSpecificationVO();
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
		exampleInstance.setCharSpecRelationship(java.util.List.of());
		exampleInstance.setCharacteristicValueSpecification(java.util.List.of());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtValueSchemaLocation("string");
		return exampleInstance;
	}
}
