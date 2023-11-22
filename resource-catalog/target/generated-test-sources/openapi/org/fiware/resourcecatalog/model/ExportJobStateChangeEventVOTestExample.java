package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ExportJobStateChangeEventVO.*;

public class ExportJobStateChangeEventVOTestExample {

	public static ExportJobStateChangeEventVO build() {
		ExportJobStateChangeEventVO exampleInstance = new ExportJobStateChangeEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ExportJobStateChangeEventPayloadVOTestExample.build());
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
