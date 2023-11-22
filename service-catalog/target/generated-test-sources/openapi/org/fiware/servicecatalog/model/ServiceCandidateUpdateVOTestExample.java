package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCandidateUpdateVO.*;

public class ServiceCandidateUpdateVOTestExample {

	public static ServiceCandidateUpdateVO build() {
		ServiceCandidateUpdateVO exampleInstance = new ServiceCandidateUpdateVO();
		//initialize fields
		exampleInstance.setDescription("string");
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
