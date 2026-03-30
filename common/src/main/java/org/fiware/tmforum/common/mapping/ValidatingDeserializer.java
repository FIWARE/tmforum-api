package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.networknt.schema.*;
import com.networknt.schema.resource.ClasspathSchemaLoader;
import com.networknt.schema.resource.UriSchemaLoader;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.SchemaValidationException;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deserializer that validates incoming objects against the linked ({@code @schemaLocation}) JSON Schema.
 *
 * <p>For known sub-types (registered via {@link SubTypePropertyProvider}), sub-type-specific properties
 * are recognized and allowed without requiring an explicit {@code @schemaLocation}. Only truly unknown
 * properties (those not belonging to any recognized sub-type) are validated against the schema or
 * rejected if no schema is provided.</p>
 */
@Slf4j
public class ValidatingDeserializer extends DelegatingDeserializer {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private final BeanDescription beanDescription;
	private final List<SubTypePropertyProvider> subTypePropertyProviders;

	/**
	 * Create a new ValidatingDeserializer.
	 *
	 * @param delegate                  the delegate deserializer
	 * @param beanDescription           the bean description for the target type
	 * @param subTypePropertyProviders  the registered sub-type property providers
	 */
	public ValidatingDeserializer(JsonDeserializer<?> delegate, BeanDescription beanDescription,
			List<SubTypePropertyProvider> subTypePropertyProviders) {
		super(delegate);
		this.beanDescription = beanDescription;
		this.subTypePropertyProviders = subTypePropertyProviders != null
				? subTypePropertyProviders : List.of();
	}

	@Override
	protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
		return new ValidatingDeserializer(newDelegatee, beanDescription, subTypePropertyProviders);
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
			Map<String, Object> unknownProperties = upb.getUnknownProperties();
			if (unknownProperties != null && !unknownProperties.isEmpty()) {
				Map<String, Object> trulyUnknown = filterKnownSubTypeProperties(
						unknownProperties, upb.getAtType());

				if (upb.getAtSchemaLocation() != null) {
					// validate only truly unknown properties against the schema
					if (!trulyUnknown.isEmpty()) {
						String unknownPropsJson = OBJECT_MAPPER.writeValueAsString(trulyUnknown);
						validateWithSchema(upb.getAtSchemaLocation(), unknownPropsJson);
					}
				} else if (!trulyUnknown.isEmpty()) {
					throw new SchemaValidationException(List.of(),
							"If no schema is provided, no additional properties are allowed.");
				}
			}
		}
		return targetObject;
	}

	/**
	 * Filter out properties that are known to belong to a recognized sub-type.
	 * Returns only the truly unknown properties that require schema validation.
	 *
	 * @param unknownProperties the unknown properties from the parent VO
	 * @param atType            the {@code @type} value from the payload, may be null
	 * @return the subset of properties that are not recognized as sub-type fields
	 */
	private Map<String, Object> filterKnownSubTypeProperties(Map<String, Object> unknownProperties,
			String atType) {
		if (atType == null || subTypePropertyProviders.isEmpty()) {
			return unknownProperties;
		}

		Set<String> knownProperties = subTypePropertyProviders.stream()
				.map(provider -> provider.getKnownProperties(atType))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.flatMap(Set::stream)
				.collect(Collectors.toSet());

		if (knownProperties.isEmpty()) {
			return unknownProperties;
		}

		Map<String, Object> trulyUnknown = new HashMap<>();
		unknownProperties.forEach((key, value) -> {
			if (!knownProperties.contains(key)) {
				trulyUnknown.put(key, value);
			}
		});
		return trulyUnknown;
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
			Set<ValidationMessage> assertions = schema.validate(jsonString, InputFormat.JSON, executionContext -> {
				executionContext.getExecutionConfig().setFormatAssertionsEnabled(true);
			});

			if (!assertions.isEmpty()) {
				log.debug("Entity {} is not valid for schema {}. Assertions: {}.", jsonString, schemaAddress, assertions);
				throw new SchemaValidationException(assertions.stream().map(ValidationMessage::getMessage).toList(), "Input is not valid for the given schema.");
			}
		} catch (Exception e) {
			if (e instanceof SchemaValidationException) {
				throw e;
			}
			throw new SchemaValidationException(List.of(), "Was not able to validate the input.", e);
		}
	}
}
