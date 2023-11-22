package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCatalogCreateVO.*;

public class ResourceCatalogCreateVOTestExample {

	public static ResourceCatalogCreateVO build() {
		ResourceCatalogCreateVO exampleInstance = new ResourceCatalogCreateVO();
		//initialize fields
		exampleInstance.setDescription("string");
		exampleInstance.setLastUpdate(null);
		exampleInstance.setLifecycleStatus("string");
		exampleInstance.setName("string");
		exampleInstance.setVersion("string");
		exampleInstance.setCategory(java.util.List.of());
		exampleInstance.setRelatedParty(java.util.List.of());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
