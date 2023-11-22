package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCategoryCreateEventPayloadVO.*;

public class ServiceCategoryCreateEventPayloadVOTestExample {

	public static ServiceCategoryCreateEventPayloadVO build() {
		ServiceCategoryCreateEventPayloadVO exampleInstance = new ServiceCategoryCreateEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceCategory(null);
		exampleInstance.setServiceCategory(ServiceCategoryVOTestExample.build());
		return exampleInstance;
	}
}
