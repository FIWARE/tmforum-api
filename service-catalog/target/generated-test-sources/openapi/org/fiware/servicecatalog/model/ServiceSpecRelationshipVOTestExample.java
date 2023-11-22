package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceSpecRelationshipVO.*;

public class ServiceSpecRelationshipVOTestExample {

	public static ServiceSpecRelationshipVO build() {
		ServiceSpecRelationshipVO exampleInstance = new ServiceSpecRelationshipVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setName("string");
		exampleInstance.setRelationshipType("string");
		exampleInstance.setRole("string");
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtReferredType("string");
		return exampleInstance;
	}
}
