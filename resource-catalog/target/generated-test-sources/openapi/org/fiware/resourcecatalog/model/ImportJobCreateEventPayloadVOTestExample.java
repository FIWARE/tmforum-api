package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ImportJobCreateEventPayloadVO.*;

public class ImportJobCreateEventPayloadVOTestExample {

	public static ImportJobCreateEventPayloadVO build() {
		ImportJobCreateEventPayloadVO exampleInstance = new ImportJobCreateEventPayloadVO();
		//initialize fields
		exampleInstance.setImportJob(null);
		exampleInstance.setImportJob(ImportJobVOTestExample.build());
		return exampleInstance;
	}
}
