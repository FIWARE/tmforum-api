package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ExportJobCreateEventPayloadVO.*;

public class ExportJobCreateEventPayloadVOTestExample {

	public static ExportJobCreateEventPayloadVO build() {
		ExportJobCreateEventPayloadVO exampleInstance = new ExportJobCreateEventPayloadVO();
		//initialize fields
		exampleInstance.setExportJob(null);
		exampleInstance.setExportJob(ExportJobVOTestExample.build());
		return exampleInstance;
	}
}
