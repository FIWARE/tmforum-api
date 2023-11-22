package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.PhysicalResourceSpecificationVO.*;

public class PhysicalResourceSpecificationVOTestExample {

	public static PhysicalResourceSpecificationVO build() {
		PhysicalResourceSpecificationVO exampleInstance = new PhysicalResourceSpecificationVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setCategory("string");
		exampleInstance.setDescription("string");
		exampleInstance.setIsBundle(false);
		exampleInstance.setLastUpdate(null);
		exampleInstance.setLifecycleStatus("string");
		exampleInstance.setModel("string");
		exampleInstance.setName("string");
		exampleInstance.setPart("string");
		exampleInstance.setSku("string");
		exampleInstance.setVendor("string");
		exampleInstance.setVersion("string");
		exampleInstance.setAttachment(java.util.List.of());
		exampleInstance.setFeatureSpecification(java.util.List.of());
		exampleInstance.setRelatedParty(java.util.List.of());
		exampleInstance.setResourceSpecCharacteristic(java.util.List.of());
		exampleInstance.setResourceSpecRelationship(java.util.List.of());
		exampleInstance.setTargetResourceSchema(null);
		exampleInstance.setTargetResourceSchema(TargetResourceSchemaVOTestExample.build());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
