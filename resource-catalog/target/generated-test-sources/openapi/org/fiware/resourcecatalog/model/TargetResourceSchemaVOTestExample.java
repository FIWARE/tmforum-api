package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.TargetResourceSchemaVO.*;

public class TargetResourceSchemaVOTestExample {

	public static TargetResourceSchemaVO build() {
		TargetResourceSchemaVO exampleInstance = new TargetResourceSchemaVO();
		//initialize fields
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
