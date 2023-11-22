package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceSpecificationChangeEventPayloadVO.*;

public class ServiceSpecificationChangeEventPayloadVOTestExample {

	public static ServiceSpecificationChangeEventPayloadVO build() {
		ServiceSpecificationChangeEventPayloadVO exampleInstance = new ServiceSpecificationChangeEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceSpecification(null);
		exampleInstance.setServiceSpecification(ServiceSpecificationVOTestExample.build());
		return exampleInstance;
	}
}
