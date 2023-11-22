package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCandidateChangeEventPayloadVO.*;

public class ServiceCandidateChangeEventPayloadVOTestExample {

	public static ServiceCandidateChangeEventPayloadVO build() {
		ServiceCandidateChangeEventPayloadVO exampleInstance = new ServiceCandidateChangeEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceCandidate(null);
		exampleInstance.setServiceCandidate(ServiceCandidateVOTestExample.build());
		return exampleInstance;
	}
}
