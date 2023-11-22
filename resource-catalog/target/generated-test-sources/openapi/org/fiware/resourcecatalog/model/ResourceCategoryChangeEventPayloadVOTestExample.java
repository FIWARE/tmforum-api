package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCategoryChangeEventPayloadVO.*;

public class ResourceCategoryChangeEventPayloadVOTestExample {

	public static ResourceCategoryChangeEventPayloadVO build() {
		ResourceCategoryChangeEventPayloadVO exampleInstance = new ResourceCategoryChangeEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceCategory(null);
		exampleInstance.setResourceCategory(ResourceCategoryVOTestExample.build());
		return exampleInstance;
	}
}
