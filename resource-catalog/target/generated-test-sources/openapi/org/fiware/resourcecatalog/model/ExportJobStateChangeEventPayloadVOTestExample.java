package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ExportJobStateChangeEventPayloadVO.*;

public class ExportJobStateChangeEventPayloadVOTestExample {

	public static ExportJobStateChangeEventPayloadVO build() {
		ExportJobStateChangeEventPayloadVO exampleInstance = new ExportJobStateChangeEventPayloadVO();
		//initialize fields
		exampleInstance.setExportJob(null);
		exampleInstance.setExportJob(ExportJobVOTestExample.build());
		return exampleInstance;
	}
}
