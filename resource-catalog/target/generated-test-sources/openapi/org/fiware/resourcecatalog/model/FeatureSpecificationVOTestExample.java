package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.FeatureSpecificationVO.*;

public class FeatureSpecificationVOTestExample {

	public static FeatureSpecificationVO build() {
		FeatureSpecificationVO exampleInstance = new FeatureSpecificationVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setIsBundle(false);
		exampleInstance.setIsEnabled(false);
		exampleInstance.setName("string");
		exampleInstance.setVersion("string");
		exampleInstance.setConstraint(java.util.List.of());
		exampleInstance.setFeatureSpecCharacteristic(java.util.List.of());
		exampleInstance.setFeatureSpecRelationship(java.util.List.of());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
