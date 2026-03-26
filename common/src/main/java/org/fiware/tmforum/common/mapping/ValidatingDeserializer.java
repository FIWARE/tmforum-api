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
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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
			if (upb.getAtSchemaLocation() != null) {
				String unknownPropsJson = OBJECT_MAPPER.writeValueAsString(upb.getUnknownProperties());
				validateWithSchema(upb.getAtSchemaLocation(), unknownPropsJson);
			} else if (upb.getUnknownProperties() != null && !upb.getUnknownProperties().isEmpty()) {
				throw new SchemaValidationException(List.of(), "If no schema is provided, no additional properties are allowed.");
			}
		}
		return targetObject;
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

		// Get all properties from BeanDescription (includes Jackson-visible properties)
		Set<String> existingProperties = new HashSet<>();
		for (BeanPropertyDefinition prop : beanDescription.findProperties()) {
			existingProperties.add(prop.getName());
		}

		// Also get all field names from the class hierarchy
		existingProperties.addAll(getAllFieldNames(beanDescription.getBeanClass()));

		// Add standard TMForum properties that should never be overridden by schemas
		existingProperties.add("id");
		existingProperties.add("href");
		existingProperties.add("@baseType");
		existingProperties.add("@type");
		existingProperties.add("@schemaLocation");

		log.debug("Final existing properties found in {}: {}", beanDescription.getBeanClass().getSimpleName(), existingProperties);

		// Collect all schema-defined properties from all possible locations
		Set<String> schemaProperties = new HashSet<>();
		Set<JsonNode> visitedNodes = new HashSet<>();
		collectAllSchemaProperties(schemaNode, schemaNode, schemaProperties, visitedNodes);

		// Check for overlaps and collect all violations
		List<String> overriddenFields = new ArrayList<>();
		for (String schemaField : schemaProperties) {
			if (existingProperties.contains(schemaField)) {
				overriddenFields.add(schemaField);
			}
		}

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

	/**
	 * Recursively collects all property names defined anywhere in the schema.
	 * Checks: properties, definitions, allOf, anyOf, oneOf, and follows $ref.
	 */
	private void collectAllSchemaProperties(JsonNode rootNode, JsonNode currentNode, Set<String> properties, Set<JsonNode> visited) {
		// Prevent infinite recursion
		if (currentNode == null || !currentNode.isObject() || visited.contains(currentNode)) {
			return;
		}
		visited.add(currentNode);

		// 1. Check direct "properties" node
		JsonNode propertiesNode = currentNode.get("properties");
		if (propertiesNode != null && propertiesNode.isObject()) {
			List<String> foundProperties = new ArrayList<>();
			propertiesNode.fieldNames().forEachRemaining(foundProperties::add);
			properties.addAll(foundProperties);
			if (!foundProperties.isEmpty()) {
				log.debug("Found properties in direct 'properties' node: {}", foundProperties);
			}
		}

		// 2. Check "definitions" node (recursively check each definition)
		JsonNode definitionsNode = currentNode.get("definitions");
		if (definitionsNode != null && definitionsNode.isObject()) {
			definitionsNode.fields().forEachRemaining(entry -> {
				JsonNode defNode = entry.getValue();
				collectAllSchemaProperties(rootNode, defNode, properties, visited);
			});
		}

		// 3. Check "allOf" combinator
		JsonNode allOfNode = currentNode.get("allOf");
		if (allOfNode != null && allOfNode.isArray()) {
			allOfNode.forEach(subSchema -> collectAllSchemaProperties(rootNode, subSchema, properties, visited));
		}

		// 4. Check "anyOf" combinator
		JsonNode anyOfNode = currentNode.get("anyOf");
		if (anyOfNode != null && anyOfNode.isArray()) {
			anyOfNode.forEach(subSchema -> collectAllSchemaProperties(rootNode, subSchema, properties, visited));
		}

		// 5. Check "oneOf" combinator
		JsonNode oneOfNode = currentNode.get("oneOf");
		if (oneOfNode != null && oneOfNode.isArray()) {
			oneOfNode.forEach(subSchema -> collectAllSchemaProperties(rootNode, subSchema, properties, visited));
		}

		// 6. Follow "$ref" references
		JsonNode refNode = currentNode.get("$ref");
		if (refNode != null && refNode.isTextual()) {
			String ref = refNode.asText();
			JsonNode resolvedNode = resolveRef(rootNode, ref);
			if (resolvedNode != null) {
				collectAllSchemaProperties(rootNode, resolvedNode, properties, visited);
			}
		}
	}

	/**
	 * Resolves a JSON Schema $ref reference.
	 * Currently supports JSON Pointer references within the same document (#/definitions/foo).
	 */
	private JsonNode resolveRef(JsonNode rootNode, String ref) {
		if (ref == null || !ref.startsWith("#/") || ref.length() <= 2) {
			log.debug("Skipping external or invalid reference: {}", ref);
			return null;
		}

		// Remove the leading "#/" and split by "/"
		String[] pathParts = ref.substring(2).split("/");
		JsonNode current = rootNode;

		for (String part : pathParts) {
			if (current == null) {
				return null;
			}
			// Skip empty parts (shouldn't happen with valid JSON Pointer)
			if (part.isEmpty()) {
				continue;
			}
			// Handle escaped characters in JSON Pointer (~ and /)
			part = part.replace("~1", "/").replace("~0", "~");
			current = current.get(part);
		}

		return current;
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

}