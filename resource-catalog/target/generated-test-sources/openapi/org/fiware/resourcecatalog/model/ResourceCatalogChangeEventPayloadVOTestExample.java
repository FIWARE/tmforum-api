package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCatalogChangeEventPayloadVO.*;

public class ResourceCatalogChangeEventPayloadVOTestExample {

	public static ResourceCatalogChangeEventPayloadVO build() {
		ResourceCatalogChangeEventPayloadVO exampleInstance = new ResourceCatalogChangeEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceCatalog(null);
		exampleInstance.setResourceCatalog(ResourceCatalogVOTestExample.build());
		return exampleInstance;
	}
}
