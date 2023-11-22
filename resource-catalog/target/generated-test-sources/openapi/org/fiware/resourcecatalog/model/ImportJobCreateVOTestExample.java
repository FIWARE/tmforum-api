package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ImportJobCreateVO.*;

public class ImportJobCreateVOTestExample {

	public static ImportJobCreateVO build() {
		ImportJobCreateVO exampleInstance = new ImportJobCreateVO();
		//initialize fields
		exampleInstance.setCompletionDate(null);
		exampleInstance.setContentType("string");
		exampleInstance.setCreationDate(null);
		exampleInstance.setErrorLog("string");
		exampleInstance.setPath("string");
		exampleInstance.setUrl(java.net.URI.create("my:uri"));
		exampleInstance.setStatus(null);
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
