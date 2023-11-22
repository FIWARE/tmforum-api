package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ErrorVO.*;

public class ErrorVOTestExample {

	public static ErrorVO build() {
		ErrorVO exampleInstance = new ErrorVO();
		//initialize fields
		exampleInstance.setCode("string");
		exampleInstance.setReason("string");
		exampleInstance.setMessage("string");
		exampleInstance.setStatus("string");
		exampleInstance.setReferenceError(java.net.URI.create("my:uri"));
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
