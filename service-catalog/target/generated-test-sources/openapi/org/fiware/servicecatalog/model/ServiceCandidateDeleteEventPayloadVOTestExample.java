package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ServiceCandidateDeleteEventPayloadVO.*;

public class ServiceCandidateDeleteEventPayloadVOTestExample {

	public static ServiceCandidateDeleteEventPayloadVO build() {
		ServiceCandidateDeleteEventPayloadVO exampleInstance = new ServiceCandidateDeleteEventPayloadVO();
		//initialize fields
		exampleInstance.setServiceCandidate(null);
		exampleInstance.setServiceCandidate(ServiceCandidateVOTestExample.build());
		return exampleInstance;
	}
}
