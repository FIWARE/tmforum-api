package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCategoryCreateVO.*;

public class ResourceCategoryCreateVOTestExample {

	public static ResourceCategoryCreateVO build() {
		ResourceCategoryCreateVO exampleInstance = new ResourceCategoryCreateVO();
		//initialize fields
		exampleInstance.setDescription("string");
		exampleInstance.setIsRoot(false);
		exampleInstance.setLastUpdate(null);
		exampleInstance.setLifecycleStatus("string");
		exampleInstance.setName("string");
		exampleInstance.setParentId("string");
		exampleInstance.setVersion("string");
		exampleInstance.setCategory(java.util.List.of());
		exampleInstance.setRelatedParty(java.util.List.of());
		exampleInstance.setResourceCandidate(java.util.List.of());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
