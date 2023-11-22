package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCategoryDeleteEventPayloadVO.*;

public class ResourceCategoryDeleteEventPayloadVOTestExample {

	public static ResourceCategoryDeleteEventPayloadVO build() {
		ResourceCategoryDeleteEventPayloadVO exampleInstance = new ResourceCategoryDeleteEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceCategory(null);
		exampleInstance.setResourceCategory(ResourceCategoryVOTestExample.build());
		return exampleInstance;
	}
}
