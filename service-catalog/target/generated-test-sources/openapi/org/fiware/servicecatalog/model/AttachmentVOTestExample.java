package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.AttachmentVO.*;

public class AttachmentVOTestExample {

	public static AttachmentVO build() {
		AttachmentVO exampleInstance = new AttachmentVO();
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
		return exampleInstance;
	}
}
