package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.RelatedPartyVO.*;

public class RelatedPartyVOTestExample {

	public static RelatedPartyVO build() {
		RelatedPartyVO exampleInstance = new RelatedPartyVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setName("string");
		exampleInstance.setRole("string");
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtReferredType("string");
		return exampleInstance;
	}
}
