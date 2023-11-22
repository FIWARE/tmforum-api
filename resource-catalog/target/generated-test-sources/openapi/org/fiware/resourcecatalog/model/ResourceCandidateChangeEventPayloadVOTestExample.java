package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCandidateChangeEventPayloadVO.*;

public class ResourceCandidateChangeEventPayloadVOTestExample {

	public static ResourceCandidateChangeEventPayloadVO build() {
		ResourceCandidateChangeEventPayloadVO exampleInstance = new ResourceCandidateChangeEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceCandidate(null);
		exampleInstance.setResourceCandidate(ResourceCandidateVOTestExample.build());
		return exampleInstance;
	}
}
