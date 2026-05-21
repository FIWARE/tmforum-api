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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Extension for the tmforum-api mappers, to handle unknown properties to extend model-vos.
 */
@Slf4j
public abstract class BaseMapper {


	private static final String PROPERTIES_KEY = "properties";
	private static final String ITEMS_KEY = "items";
	private static final String TYPE_KEY = "type";
	private static final String ARRAY_TYPE = "array";
	private static final String OBJECT_TYPE = "object";

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
				JsonNode rootSchemaNode = schema.getSchemaNode();
				var propertiesNode = rootSchemaNode.get(PROPERTIES_KEY);

				source.getAdditionalProperties()
						.forEach(additionalProperty -> {
							JsonNode propertySchemaNode = propertiesNode != null ? propertiesNode.get(additionalProperty.getName()) : null;
							Object coerced = coerceToSchema(additionalProperty.getValue(), propertySchemaNode);
							target.setUnknownProperties(additionalProperty.getName(), coerced);
						});
			} catch (Exception e) {
				log.warn("Was not able to get the schema. Will not apply special handling.", e);
				source.getAdditionalProperties()
						.forEach(additionalProperty -> target.setUnknownProperties(additionalProperty.getName(), additionalProperty.getValue()))
				;
			}
		}
	}

	/**
	 * Walk the value/schema trees in lock-step and undo the JSON-LD
	 * single-element-array compaction that NGSI-LD brokers apply to nested
	 * data: when the schema declares an attribute as {@code "type": "array"}
	 * but what we got back is a scalar (or any non-list value), wrap it into
	 * a one-element list. Recurses through both arrays ({@code items}) and
	 * objects ({@code properties}) so that the fix applies at every depth —
	 * not just the top-level attributes of the entity (which was the only
	 * level handled before).
	 */
	private static Object coerceToSchema(Object value, JsonNode schemaNode) {
		if (value == null || schemaNode == null) {
			return value;
		}
		String typeText = Optional.ofNullable(schemaNode.get(TYPE_KEY))
				.filter(TextNode.class::isInstance)
				.map(TextNode.class::cast)
				.map(TextNode::textValue)
				.orElse(null);

		if (ARRAY_TYPE.equals(typeText)) {
			JsonNode itemsNode = schemaNode.get(ITEMS_KEY);
			if (value instanceof List<?> list) {
				List<Object> rebuilt = new ArrayList<>(list.size());
				for (Object item : list) {
					rebuilt.add(coerceToSchema(item, itemsNode));
				}
				return rebuilt;
			}
			// schema says array, value is not a list → broker compacted a
			// single-element array to a scalar; wrap it back.
			return List.of(coerceToSchema(value, itemsNode));
		}

		if (OBJECT_TYPE.equals(typeText) && value instanceof Map<?, ?> map) {
			JsonNode nestedProperties = schemaNode.get(PROPERTIES_KEY);
			if (nestedProperties == null) {
				return value;
			}
			Map<String, Object> rebuilt = new LinkedHashMap<>(map.size());
			for (Map.Entry<?, ?> e : map.entrySet()) {
				String key = String.valueOf(e.getKey());
				rebuilt.put(key, coerceToSchema(e.getValue(), nestedProperties.get(key)));
			}
			return rebuilt;
		}

		return value;
	}
}
