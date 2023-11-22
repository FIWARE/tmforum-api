package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ImportJobCreateEventVO.*;

public class ImportJobCreateEventVOTestExample {

	public static ImportJobCreateEventVO build() {
		ImportJobCreateEventVO exampleInstance = new ImportJobCreateEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ImportJobCreateEventPayloadVOTestExample.build());
		exampleInstance.setEventId("string");
		exampleInstance.setEventTime(null);
		exampleInstance.setEventType("string");
		exampleInstance.setCorrelationId("string");
		exampleInstance.setDomain("string");
		exampleInstance.setTitle("string");
		exampleInstance.setDescription("string");
		exampleInstance.setPriority("string");
		exampleInstance.setTimeOcurred(null);
		return exampleInstance;
	}
}
