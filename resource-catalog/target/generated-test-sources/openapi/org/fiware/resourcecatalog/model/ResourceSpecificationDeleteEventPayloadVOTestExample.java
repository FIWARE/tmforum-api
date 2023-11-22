package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceSpecificationDeleteEventPayloadVO.*;

public class ResourceSpecificationDeleteEventPayloadVOTestExample {

	public static ResourceSpecificationDeleteEventPayloadVO build() {
		ResourceSpecificationDeleteEventPayloadVO exampleInstance = new ResourceSpecificationDeleteEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceSpecification(null);
		exampleInstance.setResourceSpecification(ResourceSpecificationVOTestExample.build());
		return exampleInstance;
	}
}
