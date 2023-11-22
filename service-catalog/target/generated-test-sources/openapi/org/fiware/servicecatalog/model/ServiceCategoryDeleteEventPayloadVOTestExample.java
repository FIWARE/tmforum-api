package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCategoryDeleteEventPayloadVO.*;

public class ServiceCategoryDeleteEventPayloadVOTestExample {

	public static ServiceCategoryDeleteEventPayloadVO build() {
		ServiceCategoryDeleteEventPayloadVO exampleInstance = new ServiceCategoryDeleteEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceCategory(null);
		exampleInstance.setServiceCategory(ServiceCategoryVOTestExample.build());
		return exampleInstance;
	}
}
