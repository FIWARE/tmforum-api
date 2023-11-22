package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.AttachmentRefOrValueVO.*;

public class AttachmentRefOrValueVOTestExample {

	public static AttachmentRefOrValueVO build() {
		AttachmentRefOrValueVO exampleInstance = new AttachmentRefOrValueVO();
		//initialize fields
		exampleInstance.setId("4aafacbd-11ff-4dc8-b445-305f2215715f");
		exampleInstance.setHref(java.net.URI.create("http://host/Attachment/4aafacbd-11ff-4dc8-b445-305f2215715f"));
		exampleInstance.setAttachmentType("video");
		exampleInstance.setContent("string");
		exampleInstance.setDescription("Photograph of the Product");
		exampleInstance.setMimeType("string");
		exampleInstance.setName("string");
		exampleInstance.setUrl(java.net.URI.create("http://host/Content/4aafacbd-11ff-4dc8-b445-305f2215715f"));
		exampleInstance.setSize(null);
		exampleInstance.setSize(QuantityVOTestExample.build());
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		exampleInstance.setAtReferredType("string");
		return exampleInstance;
	}
}
