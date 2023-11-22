package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ExportJobCreateEventVO.*;

public class ExportJobCreateEventVOTestExample {

	public static ExportJobCreateEventVO build() {
		ExportJobCreateEventVO exampleInstance = new ExportJobCreateEventVO();
		//initialize fields
		exampleInstance.setEvent(null);
		exampleInstance.setEvent(ExportJobCreateEventPayloadVOTestExample.build());
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
