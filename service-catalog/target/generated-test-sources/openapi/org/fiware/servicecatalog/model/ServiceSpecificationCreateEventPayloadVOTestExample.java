package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceSpecificationCreateEventPayloadVO.*;

public class ServiceSpecificationCreateEventPayloadVOTestExample {

	public static ServiceSpecificationCreateEventPayloadVO build() {
		ServiceSpecificationCreateEventPayloadVO exampleInstance = new ServiceSpecificationCreateEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceSpecification(null);
		exampleInstance.setServiceSpecification(ServiceSpecificationVOTestExample.build());
		return exampleInstance;
	}
}
