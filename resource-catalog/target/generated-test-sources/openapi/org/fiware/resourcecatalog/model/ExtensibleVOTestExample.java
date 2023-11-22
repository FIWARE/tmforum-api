package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.ExtensibleVO.*;

public class ExtensibleVOTestExample {

	public static ExtensibleVO build() {
		ExtensibleVO exampleInstance = new ExtensibleVO();
		//initialize fields
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
