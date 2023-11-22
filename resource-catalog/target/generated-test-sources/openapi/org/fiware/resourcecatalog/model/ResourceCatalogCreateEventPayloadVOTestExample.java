package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCatalogCreateEventPayloadVO.*;

public class ResourceCatalogCreateEventPayloadVOTestExample {

	public static ResourceCatalogCreateEventPayloadVO build() {
		ResourceCatalogCreateEventPayloadVO exampleInstance = new ResourceCatalogCreateEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceCatalog(null);
		exampleInstance.setResourceCatalog(ResourceCatalogVOTestExample.build());
		return exampleInstance;
	}
}
