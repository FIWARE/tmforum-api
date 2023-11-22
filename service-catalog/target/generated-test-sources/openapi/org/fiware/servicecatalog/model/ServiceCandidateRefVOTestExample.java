package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCandidateRefVO.*;

public class ServiceCandidateRefVOTestExample {

	public static ServiceCandidateRefVO build() {
		ServiceCandidateRefVO exampleInstance = new ServiceCandidateRefVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setName("string");
		exampleInstance.setVersion("string");
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtReferredType("string");
		return exampleInstance;
	}
}
