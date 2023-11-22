package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.FeatureSpecificationCharacteristicRelationshipVO.*;

public class FeatureSpecificationCharacteristicRelationshipVOTestExample {

	public static FeatureSpecificationCharacteristicRelationshipVO build() {
		FeatureSpecificationCharacteristicRelationshipVO exampleInstance = new FeatureSpecificationCharacteristicRelationshipVO();
		//initialize fields
		exampleInstance.setCharacteristicId("string");
		exampleInstance.setFeatureId("string");
		exampleInstance.setName("string");
		exampleInstance.setRelationshipType("string");
		exampleInstance.setResourceSpecificationHref(java.net.URI.create("my:uri"));
		exampleInstance.setResourceSpecificationId("string");
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		return exampleInstance;
	}
}
