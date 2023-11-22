package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ExportJobVO.*;

public class ExportJobVOTestExample {

	public static ExportJobVO build() {
		ExportJobVO exampleInstance = new ExportJobVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setCompletionDate(null);
		exampleInstance.setContentType("string");
		exampleInstance.setCreationDate(null);
		exampleInstance.setErrorLog("string");
		exampleInstance.setPath("string");
		exampleInstance.setQuery("string");
		exampleInstance.setUrl(java.net.URI.create("my:uri"));
		exampleInstance.setStatus(null);
		return exampleInstance;
	}
}
