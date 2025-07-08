package org.fiware.tmforum.migration.loader;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;

public class PropertyRenamingStrategy extends PropertyNamingStrategies.NamingBase {

	@Override
	public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName) {
		return rename(defaultName);
	}

	@Override
	public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
		return rename(defaultName);
	}

	@Override
	public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
		return rename(defaultName);
	}

	@Override
	public String translate(String propertyName) {
		return rename(propertyName);
	}

	private String rename(String name) {
		switch (name) {
			case "id":
				return "tmfId";
			case "value":
				return "tmfValue";
			default:
				return name;
		}
	}
}
