package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ImportJobStateChangeEventVO.*;

public class ImportJobStateChangeEventVOTestExample {

	public static ImportJobStateChangeEventVO build() {
		ImportJobStateChangeEventVO exampleInstance = new ImportJobStateChangeEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ImportJobStateChangeEventPayloadVOTestExample.build());
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
