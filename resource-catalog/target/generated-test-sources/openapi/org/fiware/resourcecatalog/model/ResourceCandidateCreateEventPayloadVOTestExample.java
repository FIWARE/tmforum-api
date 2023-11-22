package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCandidateCreateEventPayloadVO.*;

public class ResourceCandidateCreateEventPayloadVOTestExample {

	public static ResourceCandidateCreateEventPayloadVO build() {
		ResourceCandidateCreateEventPayloadVO exampleInstance = new ResourceCandidateCreateEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceCandidate(null);
		exampleInstance.setResourceCandidate(ResourceCandidateVOTestExample.build());
		return exampleInstance;
	}
}
