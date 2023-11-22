package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCatalogDeleteEventPayloadVO.*;

public class ResourceCatalogDeleteEventPayloadVOTestExample {

	public static ResourceCatalogDeleteEventPayloadVO build() {
		ResourceCatalogDeleteEventPayloadVO exampleInstance = new ResourceCatalogDeleteEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceCatalog(null);
		exampleInstance.setResourceCatalog(ResourceCatalogVOTestExample.build());
		return exampleInstance;
	}
}
