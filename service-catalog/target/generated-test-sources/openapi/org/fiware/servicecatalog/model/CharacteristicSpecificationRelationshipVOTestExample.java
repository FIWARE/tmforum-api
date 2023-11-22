package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.CharacteristicSpecificationRelationshipVO.*;

public class CharacteristicSpecificationRelationshipVOTestExample {

	public static CharacteristicSpecificationRelationshipVO build() {
		CharacteristicSpecificationRelationshipVO exampleInstance = new CharacteristicSpecificationRelationshipVO();
		//initialize fields
		exampleInstance.setCharacteristicSpecificationId("string");
		exampleInstance.setName("string");
		exampleInstance.setParentSpecificationHref(java.net.URI.create("my:uri"));
		exampleInstance.setParentSpecificationId("string");
		exampleInstance.setRelationshipType("string");
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		return exampleInstance;
	}
}
