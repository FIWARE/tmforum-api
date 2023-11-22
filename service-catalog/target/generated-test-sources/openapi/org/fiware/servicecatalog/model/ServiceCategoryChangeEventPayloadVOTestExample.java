package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCategoryChangeEventPayloadVO.*;

public class ServiceCategoryChangeEventPayloadVOTestExample {

	public static ServiceCategoryChangeEventPayloadVO build() {
		ServiceCategoryChangeEventPayloadVO exampleInstance = new ServiceCategoryChangeEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceCategory(null);
		exampleInstance.setServiceCategory(ServiceCategoryVOTestExample.build());
		return exampleInstance;
	}
}
