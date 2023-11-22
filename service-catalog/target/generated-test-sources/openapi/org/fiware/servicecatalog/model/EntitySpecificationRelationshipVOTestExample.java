package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.EntitySpecificationRelationshipVO.*;

public class EntitySpecificationRelationshipVOTestExample {

	public static EntitySpecificationRelationshipVO build() {
		EntitySpecificationRelationshipVO exampleInstance = new EntitySpecificationRelationshipVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setName("string");
		exampleInstance.setRelationshipType("string");
		exampleInstance.setRole("string");
		exampleInstance.setAssociationSpec(null);
		exampleInstance.setAssociationSpec(AssociationSpecificationRefVOTestExample.build());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtReferredType("string");
		return exampleInstance;
	}
}
