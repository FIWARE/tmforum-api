package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.FeatureSpecificationRelationshipVO.*;

public class FeatureSpecificationRelationshipVOTestExample {

	public static FeatureSpecificationRelationshipVO build() {
		FeatureSpecificationRelationshipVO exampleInstance = new FeatureSpecificationRelationshipVO();
		//initialize fields
		exampleInstance.setFeatureId("string");
		exampleInstance.setName("string");
		exampleInstance.setParentSpecificationHref(java.net.URI.create("my:uri"));
		exampleInstance.setParentSpecificationId("string");
		exampleInstance.setRelationshipType("string");
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		return exampleInstance;
	}
}
