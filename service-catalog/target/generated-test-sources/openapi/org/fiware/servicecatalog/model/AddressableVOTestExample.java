package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.AddressableVO.*;

public class AddressableVOTestExample {

	public static AddressableVO build() {
		AddressableVO exampleInstance = new AddressableVO();
		//initialize fields
		exampleInstance.setId("string");
		exampleInstance.setHref(java.net.URI.create("my:uri"));
		return exampleInstance;
	}
}
