package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ImportJobStateChangeEventPayloadVO.*;

public class ImportJobStateChangeEventPayloadVOTestExample {

	public static ImportJobStateChangeEventPayloadVO build() {
		ImportJobStateChangeEventPayloadVO exampleInstance = new ImportJobStateChangeEventPayloadVO();
		//initialize fields
		exampleInstance.setImportJob(null);
		exampleInstance.setImportJob(ImportJobVOTestExample.build());
		return exampleInstance;
	}
}
