package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCategoryCreateEventPayloadVO.*;

public class ResourceCategoryCreateEventPayloadVOTestExample {

	public static ResourceCategoryCreateEventPayloadVO build() {
		ResourceCategoryCreateEventPayloadVO exampleInstance = new ResourceCategoryCreateEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceCategory(null);
		exampleInstance.setResourceCategory(ResourceCategoryVOTestExample.build());
		return exampleInstance;
	}
}
