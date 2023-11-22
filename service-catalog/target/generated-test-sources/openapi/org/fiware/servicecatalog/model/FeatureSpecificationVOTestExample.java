package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.FeatureSpecificationVO.*;

public class FeatureSpecificationVOTestExample {

	public static FeatureSpecificationVO build() {
		FeatureSpecificationVO exampleInstance = new FeatureSpecificationVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setIsBundle(false);
		exampleInstance.setIsEnabled(false);
		exampleInstance.setName("string");
		exampleInstance.setVersion("string");
		exampleInstance.setConstraint(java.util.List.of());
		exampleInstance.setFeatureSpecCharacteristic(java.util.List.of());
		exampleInstance.setFeatureSpecRelationship(java.util.List.of());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		return exampleInstance;
	}
}
