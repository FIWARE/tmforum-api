package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.networknt.schema.*;
import com.networknt.schema.resource.ClasspathSchemaLoader;
import com.networknt.schema.resource.SchemaMapper;
import com.networknt.schema.resource.UriSchemaLoader;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.context.ServerRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.SchemaValidationException;

import javax.validation.ValidationException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deserializer, that validates incoming objects against the linked(atSchemaLocation) json-schema.
 */
@Slf4j
public class ValidatingDeserializer extends DelegatingDeserializer {
	private static final String JSON_SCHEMA_PROPERTIES_KEY = "properties";

	private final BeanDescription beanDescription;

	public ValidatingDeserializer(JsonDeserializer<?> d, BeanDescription beanDescription) {
		super(d);
		this.beanDescription = beanDescription;
	}

	@Override
	protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
		return new ValidatingDeserializer(newDelegatee, beanDescription);

	}

	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		PropertyName schemaLocationProperty = new PropertyName("@schemaLocation");
		if (beanDescription.findProperties().stream().noneMatch(bpd -> bpd.hasName(schemaLocationProperty))) {
			return super.deserialize(p, ctxt);
		}
		// copy the current value into the buffer, so that we can re-read it to validate the schema.
		TokenBuffer tokenBuffer = ctxt.bufferAsCopyOfValue(p);
		Object targetObject = super.deserialize(tokenBuffer.asParserOnFirstToken(), ctxt);
		if (targetObject instanceof UnknownPreservingBase upb) {
			// Check if unknown properties contain fields from the base VO class, check if entity has id, href...
			checkForBaseVoFieldsInUnknownProperties(upb);

			if (upb.getAtSchemaLocation() != null) {
				validateWithSchema(upb.getAtSchemaLocation(), tokenBuffer.asParserOnFirstToken().readValueAsTree().toString());
			} else if (upb.getUnknownProperties() != null && !upb.getUnknownProperties().isEmpty()) {
				throw new SchemaValidationException(List.of(), "If no schema is provided, no additional properties are allowed.");
			}
		}
		return targetObject;
	}

	/**
	 * Checks if unknown properties contain fields that exist in the base VO class detect when existing fields
	 * are passed in the request and rejects them
	 */
	private void checkForBaseVoFieldsInUnknownProperties(UnknownPreservingBase upb) {
		if (upb.getUnknownProperties() == null || upb.getUnknownProperties().isEmpty()) {
			return;
		}

		Class<?> baseVoClass = findBaseVoClass(beanDescription.getBeanClass());
		if (baseVoClass == null) {
			return;
		}

		Set<String> baseVoFields = getAllFieldNames(baseVoClass);
		List<String> forbiddenFields = upb.getUnknownProperties().keySet().stream()
				.filter(baseVoFields::contains)
				.toList();

		if (!forbiddenFields.isEmpty()) {
			String entityName = beanDescription.getBeanClass().getSimpleName();
			String errorMessage = String.format(
				"Invalid request for '%s': The following fields are server-generated and cannot be set: %s",
				entityName,
				String.join(", ", forbiddenFields)
			);
			log.error(errorMessage);
			throw new SchemaValidationException(List.of(errorMessage), errorMessage);
		}
	}

	private void validateWithSchema(Object theSchema, String jsonString) {
		String schemaAddress = "";
		if (theSchema instanceof URI schemaUri) {
			schemaAddress = schemaUri.toString();
		} else if (theSchema instanceof String schemaString) {
			schemaAddress = schemaString;
		} else {
			throw new SchemaValidationException(List.of(), "No valid schema address was provided");
		}

		try {
			JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(
					SpecVersion.VersionFlag.V202012,
					builder -> builder.schemaLoaders(sb -> {
						sb.add(new ClasspathSchemaLoader());
						sb.add(new UriSchemaLoader());
					})
			);
			SchemaValidatorsConfig.Builder validatorConfigBuilder = SchemaValidatorsConfig.builder();
			SchemaValidatorsConfig schemaValidatorsConfig = validatorConfigBuilder.build();
			JsonSchema schema = jsonSchemaFactory.getSchema(SchemaLocation.of(schemaAddress), schemaValidatorsConfig);
			checkForExplicitFieldOverrides(schema);
			Set<ValidationMessage> assertions = schema.validate(jsonString, InputFormat.JSON, executionContext -> {
				executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
			});

			if (!assertions.isEmpty()) {
				log.debug("Entity {} is not valid for schema {}. Assertions: {}.", jsonString, schemaAddress, assertions);
				throw new SchemaValidationException(assertions.stream().map(ValidationMessage::getMessage).toList(), "Input is not valid for the given schema.");
			}
		} catch (Exception e) {
			if(e instanceof SchemaValidationException) {
				throw e;
			}
			throw new SchemaValidationException(List.of(), "Was not able to validate the input.", e);
		}

	}

	private void checkForExplicitFieldOverrides(JsonSchema schema) {
		var schemaNode = schema.getSchemaNode();
		var propertiesNode = schemaNode.get(JSON_SCHEMA_PROPERTIES_KEY);

		if (propertiesNode == null || !propertiesNode.isObject()) {
			log.debug("Schema has no properties node, skipping override check");
			return;
		}

		// Get all properties from BeanDescription (includes Jackson-visible properties)
		Set<String> existingProperties = new HashSet<>();
		for (BeanPropertyDefinition prop : beanDescription.findProperties()) {
			existingProperties.add(prop.getName());
		}

		// Also get all field names from the class hierarchy
		existingProperties.addAll(getAllFieldNames(beanDescription.getBeanClass()));

		Class<?> baseVoClass = findBaseVoClass(beanDescription.getBeanClass());
		if (baseVoClass != null) {
			existingProperties.addAll(getAllFieldNames(baseVoClass));
			log.debug("Added fields from base VO class {}: {}", baseVoClass.getSimpleName(), getAllFieldNames(baseVoClass));
		}

		log.debug("Final existing properties found in {}: {}", beanDescription.getBeanClass().getSimpleName(), existingProperties);

		// Check for overlaps and collect all violations
		List<String> overriddenFields = new ArrayList<>();
		propertiesNode.fieldNames().forEachRemaining(schemaField -> {
			if (existingProperties.contains(schemaField)) {
				overriddenFields.add(schemaField);
			}
		});

		// If any fields are being overridden, throw an error
		if (!overriddenFields.isEmpty()) {
			String entityName = beanDescription.getBeanClass().getSimpleName();
			String errorMessage = String.format(
				"Schema validation failed for entity '%s': The schema attempts to override the following base properties: %s. " +
				"These properties are part of the entity definition and cannot be redefined in the schema.",
				entityName,
				String.join(", ", overriddenFields)
			);
			log.error(errorMessage);
			throw new SchemaValidationException(List.of(errorMessage), errorMessage);
		}
	}

	private Set<String> getAllFieldNames(Class<?> clazz) {
		Set<String> fieldNames = new HashSet<>();
		Class<?> current = clazz;

		log.debug("Getting all fields for class: {}", clazz.getName());

		// Traverse the class hierarchy
		while (current != null && current != Object.class) {
			log.debug("  Checking class: {}", current.getSimpleName());
			java.lang.reflect.Field[] fields = current.getDeclaredFields();
			log.debug("    Found {} fields", fields.length);
			for (java.lang.reflect.Field field : fields) {
				log.debug("      Field: {} (type: {})", field.getName(), field.getType().getSimpleName());
				fieldNames.add(field.getName());
			}
			current = current.getSuperclass();
			if (current != null && current != Object.class) {
				log.debug("    Moving to superclass: {}", current.getSimpleName());
			}
		}

		return fieldNames;
	}

	/**
	 * Finds the corresponding base VO class for VOs.
	 */
	private Class<?> findBaseVoClass(Class<?> voClass) {
		String className = voClass.getName();
		String baseClassName = null;

		if (className.endsWith("CreateVO")) {
			baseClassName = className.substring(0, className.length() - "CreateVO".length()) + "VO";
		} else if (className.endsWith("UpdateVO")) {
			baseClassName = className.substring(0, className.length() - "UpdateVO".length()) + "VO";
		}

		if (baseClassName != null) {
			try {
				Class<?> baseClass = Class.forName(baseClassName);
				log.debug("Found base VO class {} for {}", baseClass.getSimpleName(), voClass.getSimpleName());
				return baseClass;
			} catch (ClassNotFoundException e) {
				log.debug("No base VO class found for {} (tried {})", voClass.getSimpleName(), baseClassName);
			}
		}

		return null;
	}

}
