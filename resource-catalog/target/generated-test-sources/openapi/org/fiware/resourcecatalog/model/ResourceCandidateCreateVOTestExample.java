package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCandidateCreateVO.*;

public class ResourceCandidateCreateVOTestExample {

	public static ResourceCandidateCreateVO build() {
		ResourceCandidateCreateVO exampleInstance = new ResourceCandidateCreateVO();
		//initialize fields
		exampleInstance.setDescription("string");
		exampleInstance.setLastUpdate(null);
		exampleInstance.setLifecycleStatus("string");
		exampleInstance.setName("string");
		exampleInstance.setVersion("string");
		exampleInstance.setCategory(java.util.List.of());
		exampleInstance.setResourceSpecification(null);
		exampleInstance.setResourceSpecification(ResourceSpecificationRefVOTestExample.build());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
