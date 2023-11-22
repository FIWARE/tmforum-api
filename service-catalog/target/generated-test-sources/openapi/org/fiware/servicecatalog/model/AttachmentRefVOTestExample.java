package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.AttachmentRefVO.*;

public class AttachmentRefVOTestExample {

	public static AttachmentRefVO build() {
		AttachmentRefVO exampleInstance = new AttachmentRefVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setDescription("string");
		exampleInstance.setName("string");
		exampleInstance.setUrl(java.net.URI.create("my:uri"));
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtReferredType("string");
		return exampleInstance;
	}
}
