package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.FeatureSpecificationRelationshipVO.*;

public class FeatureSpecificationRelationshipVOTestExample {

	public static FeatureSpecificationRelationshipVO build() {
		FeatureSpecificationRelationshipVO exampleInstance = new FeatureSpecificationRelationshipVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setFeatureId("string");
		exampleInstance.setName("string");
		exampleInstance.setParentSpecificationHref(java.net.URI.create("my:uri"));
		exampleInstance.setParentSpecificationId("string");
		exampleInstance.setRelationshipType("string");
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
