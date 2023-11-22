package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ImportJobCreateVO.*;

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
		return exampleInstance;
	}
}
