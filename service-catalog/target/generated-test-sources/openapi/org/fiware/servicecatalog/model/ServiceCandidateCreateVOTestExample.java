package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCandidateCreateVO.*;

public class ServiceCandidateCreateVOTestExample {

	public static ServiceCandidateCreateVO build() {
		ServiceCandidateCreateVO exampleInstance = new ServiceCandidateCreateVO();
		//initialize fields
		exampleInstance.setDescription("string");
		exampleInstance.setLastUpdate(null);
		exampleInstance.setLifecycleStatus("string");
		exampleInstance.setName("string");
		exampleInstance.setVersion("string");
		exampleInstance.setCategory(java.util.List.of());
		exampleInstance.setServiceSpecification(null);
		exampleInstance.setServiceSpecification(ServiceSpecificationRefVOTestExample.build());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
