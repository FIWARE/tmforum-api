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
	private static final String JSON_SCHEMA_DEFINITIONS_KEY = "definitions";
	private static final String JSON_SCHEMA_ALL_OF_KEY = "allOf";
	private static final String JSON_SCHEMA_ANY_OF_KEY = "anyOf";
	private static final String JSON_SCHEMA_ONE_OF_KEY = "oneOf";
	private static final String JSON_SCHEMA_REF_KEY = "$ref";

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
			checkForBaseVoFieldsInUnknownProperties(upb, ctxt);

			if (upb.getAtSchemaLocation() != null) {
				validateWithSchema(upb.getAtSchemaLocation(), tokenBuffer.asParserOnFirstToken().readValueAsTree().toString(), ctxt);
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
	private void checkForBaseVoFieldsInUnknownProperties(UnknownPreservingBase upb, DeserializationContext ctxt) {
		if (upb.getUnknownProperties() == null || upb.getUnknownProperties().isEmpty()) {
			return;
		}

		Class<?> baseVoClass = findBaseVoClass(beanDescription.getBeanClass());
		if (baseVoClass == null) {
			return;
		}

		BeanDescription baseVoBeanDesc = ctxt.getConfig().introspect(ctxt.constructType(baseVoClass));
		Set<String> baseVoFields = baseVoBeanDesc.findProperties().stream()
				.map(BeanPropertyDefinition::getName)
				.collect(Collectors.toSet());

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

	private void validateWithSchema(Object theSchema, String jsonString, DeserializationContext ctxt) {
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
			checkForExplicitFieldOverrides(schema, ctxt);
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

	private void checkForExplicitFieldOverrides(JsonSchema schema, DeserializationContext ctxt) {
		var schemaNode = schema.getSchemaNode();

		// Get all Jackson-visible property names for the current class (respects @JsonProperty annotations)
		Set<String> existingProperties = beanDescription.findProperties().stream()
				.map(BeanPropertyDefinition::getName)
				.collect(Collectors.toCollection(HashSet::new));

		// For CreateVO/UpdateVO, also include the base VO's Jackson property names
		Class<?> baseVoClass = findBaseVoClass(beanDescription.getBeanClass());
		if (baseVoClass != null) {
			BeanDescription baseVoBeanDesc = ctxt.getConfig().introspect(ctxt.constructType(baseVoClass));
			Set<String> baseVoProperties = baseVoBeanDesc.findProperties().stream()
					.map(BeanPropertyDefinition::getName)
					.collect(Collectors.toSet());
			existingProperties.addAll(baseVoProperties);
			log.debug("Added Jackson properties from base VO class {}: {}", baseVoClass.getSimpleName(), baseVoProperties);
		}

		log.debug("Final existing properties found in {}: {}", beanDescription.getBeanClass().getSimpleName(), existingProperties);

		// Collect all schema-defined properties from all possible locations (deep traversal)
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
		JsonNode propertiesNode = currentNode.get(JSON_SCHEMA_PROPERTIES_KEY);
		if (propertiesNode != null && propertiesNode.isObject()) {
			List<String> foundProperties = new ArrayList<>();
			propertiesNode.fieldNames().forEachRemaining(foundProperties::add);
			properties.addAll(foundProperties);
			if (!foundProperties.isEmpty()) {
				log.debug("Found properties in direct 'properties' node: {}", foundProperties);
			}
		}

		// 2. Check "definitions" node (recursively check each definition)
		JsonNode definitionsNode = currentNode.get(JSON_SCHEMA_DEFINITIONS_KEY);
		if (definitionsNode != null && definitionsNode.isObject()) {
			definitionsNode.fields().forEachRemaining(entry ->
					collectAllSchemaProperties(rootNode, entry.getValue(), properties, visited));
		}

		// 3. Check "allOf" combinator
		JsonNode allOfNode = currentNode.get(JSON_SCHEMA_ALL_OF_KEY);
		if (allOfNode != null && allOfNode.isArray()) {
			allOfNode.forEach(subSchema -> collectAllSchemaProperties(rootNode, subSchema, properties, visited));
		}

		// 4. Check "anyOf" combinator
		JsonNode anyOfNode = currentNode.get(JSON_SCHEMA_ANY_OF_KEY);
		if (anyOfNode != null && anyOfNode.isArray()) {
			anyOfNode.forEach(subSchema -> collectAllSchemaProperties(rootNode, subSchema, properties, visited));
		}

		// 5. Check "oneOf" combinator
		JsonNode oneOfNode = currentNode.get(JSON_SCHEMA_ONE_OF_KEY);
		if (oneOfNode != null && oneOfNode.isArray()) {
			oneOfNode.forEach(subSchema -> collectAllSchemaProperties(rootNode, subSchema, properties, visited));
		}

		// 6. Follow "$ref" references
		JsonNode refNode = currentNode.get(JSON_SCHEMA_REF_KEY);
		if (refNode != null && refNode.isTextual()) {
			JsonNode resolvedNode = resolveRef(rootNode, refNode.asText());
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

		String[] pathParts = ref.substring(2).split("/");
		JsonNode current = rootNode;

		for (String part : pathParts) {
			if (current == null || part.isEmpty()) {
				continue;
			}
			// Handle escaped characters in JSON Pointer (~ and /)
			current = current.get(part.replace("~1", "/").replace("~0", "~"));
		}

		return current;
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
