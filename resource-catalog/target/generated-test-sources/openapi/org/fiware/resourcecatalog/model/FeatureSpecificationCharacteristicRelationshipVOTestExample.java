package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.FeatureSpecificationCharacteristicRelationshipVO.*;

public class FeatureSpecificationCharacteristicRelationshipVOTestExample {

	public static FeatureSpecificationCharacteristicRelationshipVO build() {
		FeatureSpecificationCharacteristicRelationshipVO exampleInstance = new FeatureSpecificationCharacteristicRelationshipVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setCharacteristicId("string");
		exampleInstance.setFeatureId("string");
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
