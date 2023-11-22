package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.EntitySpecificationVO.*;

public class EntitySpecificationVOTestExample {

	public static EntitySpecificationVO build() {
		EntitySpecificationVO exampleInstance = new EntitySpecificationVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setDescription("string");
		exampleInstance.setIsBundle(false);
		exampleInstance.setLastUpdate(null);
		exampleInstance.setLifecycleStatus("string");
		exampleInstance.setName("string");
		exampleInstance.setVersion("string");
		exampleInstance.setAttachment(java.util.List.of());
		exampleInstance.setConstraint(java.util.List.of());
		exampleInstance.setEntitySpecRelationship(java.util.List.of());
		exampleInstance.setRelatedParty(java.util.List.of());
		exampleInstance.setSpecCharacteristic(java.util.List.of());
		exampleInstance.setTargetEntitySchema(null);
		exampleInstance.setTargetEntitySchema(TargetEntitySchemaVOTestExample.build());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
