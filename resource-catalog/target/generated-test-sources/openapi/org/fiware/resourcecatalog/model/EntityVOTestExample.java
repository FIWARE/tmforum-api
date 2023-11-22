package org.fiware.resourcecatalog.model;

import org.fiware.resourcecatalog.model.EntityVO.*;

public class EntityVOTestExample {

	public static EntityVO build() {
		EntityVO exampleInstance = new EntityVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
