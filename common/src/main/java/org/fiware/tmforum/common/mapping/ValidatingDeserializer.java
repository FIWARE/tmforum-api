package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deserializer, that validates incoming objects against the linked(atSchemaLocation) json-schema.
 */
@Slf4j
public class ValidatingDeserializer extends DelegatingDeserializer {
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
				validateWithSchema(upb.getAtSchemaLocation(), tokenBuffer.asParserOnFirstToken().readValueAsTree().toString());
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
	}
}
