package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.*;
import com.networknt.schema.resource.ClasspathSchemaLoader;
import com.networknt.schema.resource.UriSchemaLoader;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.domain.Characteristic;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.Money;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Optional;

/**
 * Extension for the tmforum-api mappers, to handle unknown properties to extend model-vos.
 */
@Slf4j
public abstract class BaseMapper {


	private static final String PROPERTIES_KEY = "properties";
	private static final String TYPE_KEY = "type";
	private static final String ARRAY_TYPE = "array";

	@AfterMapping
	public void afterMappingToEntity(UnknownPreservingBase source, @MappingTarget Entity e) {
		if (source.getAtSchemaLocation() != null && source.getUnknownProperties() != null) {
			source.getUnknownProperties().forEach(e::addAdditionalProperties);
		}
	}

	@AfterMapping
	public void afterMappingFromEntity(Entity source, @MappingTarget UnknownPreservingBase target) {
		if (source.getAtSchemaLocation() != null && source.getAdditionalProperties() != null) {
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
				JsonSchema schema = jsonSchemaFactory.getSchema(SchemaLocation.of(source.getAtSchemaLocation().toString()), schemaValidatorsConfig);
				var propertiesNode = schema.getSchemaNode().get(PROPERTIES_KEY);

				source.getAdditionalProperties()
						.forEach(additionalProperty -> {
							if (!(additionalProperty.getValue() instanceof List) && isArray(additionalProperty.getName(), propertiesNode) && additionalProperty.getValue() != null) {
								// since json-ld flattens single element lists to plain objects, we have rebuild them to single element lists if the schema requires it.
								target.setUnknownProperties(additionalProperty.getName(), List.of(additionalProperty.getValue()));
							} else {
								target.setUnknownProperties(additionalProperty.getName(), additionalProperty.getValue());
							}
						});
			} catch (Exception e) {
				log.warn("Was not able to get the schema. Will not apply special handling.", e);
				source.getAdditionalProperties()
						.forEach(additionalProperty -> target.setUnknownProperties(additionalProperty.getName(), additionalProperty.getValue()))
				;
			}
		}
	}

	private static boolean isArray(String propertyName, JsonNode properties) {
		return Optional.ofNullable(properties.get(propertyName))
				.map(node -> node.get(TYPE_KEY))
				.filter(TextNode.class::isInstance)
				.map(TextNode.class::cast)
				.map(TextNode::textValue)
				.filter(type -> type.equals(ARRAY_TYPE))
				.isPresent();
	}
}
