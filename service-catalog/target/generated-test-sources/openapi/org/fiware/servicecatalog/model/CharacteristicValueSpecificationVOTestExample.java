package org.fiware.servicecatalog.model;

import org.fiware.servicecatalog.model.CharacteristicValueSpecificationVO.*;

public class CharacteristicValueSpecificationVOTestExample {

	public static CharacteristicValueSpecificationVO build() {
		CharacteristicValueSpecificationVO exampleInstance = new CharacteristicValueSpecificationVO();
		//initialize fields
		exampleInstance.setIsDefault(false);
		exampleInstance.setRangeInterval("string");
		exampleInstance.setRegex("string");
		exampleInstance.setUnitOfMeasure("string");
		exampleInstance.setValueFrom(12);
		exampleInstance.setValueTo(12);
		exampleInstance.setValueType("string");
		exampleInstance.setValidFor(null);
		exampleInstance.setValidFor(TimePeriodVOTestExample.build());
		exampleInstance.setValue(null);
		exampleInstance.setAtBaseType("string");
		exampleInstance.setAtSchemaLocation(java.net.URI.create("my:uri"));
		exampleInstance.setAtType("string");
		return exampleInstance;
	}
}
