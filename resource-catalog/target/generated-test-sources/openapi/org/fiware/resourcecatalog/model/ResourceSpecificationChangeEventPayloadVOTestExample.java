package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceSpecificationChangeEventPayloadVO.*;

public class ResourceSpecificationChangeEventPayloadVOTestExample {

	public static ResourceSpecificationChangeEventPayloadVO build() {
		ResourceSpecificationChangeEventPayloadVO exampleInstance = new ResourceSpecificationChangeEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceSpecification(null);
		exampleInstance.setResourceSpecification(ResourceSpecificationVOTestExample.build());
		return exampleInstance;
	}
}
