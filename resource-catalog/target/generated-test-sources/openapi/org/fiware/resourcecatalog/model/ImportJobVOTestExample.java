package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ImportJobVO.*;

public class ImportJobVOTestExample {

	public static ImportJobVO build() {
		ImportJobVO exampleInstance = new ImportJobVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
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
