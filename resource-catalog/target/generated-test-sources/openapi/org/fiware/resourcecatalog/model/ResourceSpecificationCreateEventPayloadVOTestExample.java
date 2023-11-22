package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceSpecificationCreateEventPayloadVO.*;

public class ResourceSpecificationCreateEventPayloadVOTestExample {

	public static ResourceSpecificationCreateEventPayloadVO build() {
		ResourceSpecificationCreateEventPayloadVO exampleInstance = new ResourceSpecificationCreateEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceSpecification(null);
		exampleInstance.setResourceSpecification(ResourceSpecificationVOTestExample.build());
		return exampleInstance;
	}
}
