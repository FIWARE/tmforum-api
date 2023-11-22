package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ResourceCandidateDeleteEventPayloadVO.*;

public class ResourceCandidateDeleteEventPayloadVOTestExample {

	public static ResourceCandidateDeleteEventPayloadVO build() {
		ResourceCandidateDeleteEventPayloadVO exampleInstance = new ResourceCandidateDeleteEventPayloadVO();
		//initialize fields
		exampleInstance.setResourceCandidate(null);
		exampleInstance.setResourceCandidate(ResourceCandidateVOTestExample.build());
		return exampleInstance;
	}
}
