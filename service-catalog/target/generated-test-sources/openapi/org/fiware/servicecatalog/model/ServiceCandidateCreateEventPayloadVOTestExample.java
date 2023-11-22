package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCandidateCreateEventPayloadVO.*;

public class ServiceCandidateCreateEventPayloadVOTestExample {

	public static ServiceCandidateCreateEventPayloadVO build() {
		ServiceCandidateCreateEventPayloadVO exampleInstance = new ServiceCandidateCreateEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceCandidate(null);
		exampleInstance.setServiceCandidate(ServiceCandidateVOTestExample.build());
		return exampleInstance;
	}
}
