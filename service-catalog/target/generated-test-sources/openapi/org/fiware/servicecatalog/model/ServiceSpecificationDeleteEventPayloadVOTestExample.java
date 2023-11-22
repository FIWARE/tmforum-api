package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceSpecificationDeleteEventPayloadVO.*;

public class ServiceSpecificationDeleteEventPayloadVOTestExample {

	public static ServiceSpecificationDeleteEventPayloadVO build() {
		ServiceSpecificationDeleteEventPayloadVO exampleInstance = new ServiceSpecificationDeleteEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceSpecification(null);
		exampleInstance.setServiceSpecification(ServiceSpecificationVOTestExample.build());
		return exampleInstance;
	}
}
