package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.ExtensibleVO.*;

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
