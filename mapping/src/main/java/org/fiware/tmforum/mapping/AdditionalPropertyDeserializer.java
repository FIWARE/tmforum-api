package org.fiware.tmforum.mapping;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.AsArrayTypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.fiware.ngsi.model.AdditionalPropertyObjectVO;
import org.fiware.ngsi.model.GeoPropertyListVO;
import org.fiware.ngsi.model.GeoPropertyVO;
import org.fiware.ngsi.model.PropertyListVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipListVO;
import org.fiware.ngsi.model.RelationshipVO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fiware.tmforum.mapping.AdditionalPropertyTypeResolver.NGSI_LD_TYPE_PROPERTY_NAME;

/**
 * Custom deserializer for the {@link org.fiware.ngsi.model.AdditionalPropertyVO}
 * It can differentiate between list and object type properties and handle them with the fitting serializer.
 */
public class AdditionalPropertyDeserializer extends AsArrayTypeDeserializer {

	/**
	 * Default property-type deserializer can be used for the individual objects.
	 */
	private final AsPropertyTypeDeserializer additionalPropertyObjectDeser;

	public AdditionalPropertyDeserializer(JavaType bt, TypeIdResolver idRes, String typePropertyName, boolean typeIdVisible, JavaType defaultImpl) {
		super(bt, idRes, typePropertyName, typeIdVisible, defaultImpl);
		additionalPropertyObjectDeser = new AsPropertyTypeDeserializer(
				TypeFactory.defaultInstance().constructType(new TypeReference<AdditionalPropertyObjectVO>() {
				}),
				idRes,
				NGSI_LD_TYPE_PROPERTY_NAME,
				false,
				defaultImpl);

	}

	@Override
	protected Object _deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

		// default behaviour
		if (p.canReadTypeId()) {
			Object typeId = p.getTypeId();
			if (typeId != null) {
				return _deserializeWithNativeTypeId(p, ctxt, typeId);
			}
		}
		// if no type id is present we need the next token to decide if its an object or an array
		JsonToken t = p.nextToken();
		// case FIELD_NAME:
		// If the token is of type FIELD_NAME, no START_ARRAY was present, thus we can take the
		// object and directly serialize it with the standard deserializer
		if (t == JsonToken.FIELD_NAME) {
			return additionalPropertyObjectDeser.deserializeTypedFromObject(p, ctxt);
		}
		// case START_OBJECT
		// If a start-object(e.g. '{') token is present, a START_ARRAY was present and we have at least one
		// one object to be deserialized. The parser is handed over to the specialized method.
		if (t == JsonToken.START_OBJECT) {
			return deserializeArray(p, ctxt);

		}
		return super._deserialize(p, ctxt);
	}

	/**
	 * Deserializes an array of NGSI-LD properties(e.g. Property, GeoProperty or Relationship) to there concrete
	 * list types(e.g. PropertyList, GeoPropertyList, RelationshipList)
	 * @param p the current parser
	 * @param ctxt the desrialization context to use
	 * @return object of the deserialized list, will be a PropertyList, GeoPropertyList or RelationshipList type
	 * @throws IOException
	 */
	private Object deserializeArray(JsonParser p, DeserializationContext ctxt) throws IOException {
		List<Object> deserializedObjects = new ArrayList<>();
		JsonToken next;
		do {
			next = p.nextToken();
			if (next == JsonToken.FIELD_NAME) {
				deserializedObjects.add(additionalPropertyObjectDeser.deserializeTypedFromObject(p, ctxt));
			}
		} while (next != JsonToken.END_ARRAY);

		if (deserializedObjects.isEmpty()) {
			// any empty list is sufficient
			return new PropertyListVO();
		}
		// get type of first object to decide the list type
		Object firstObject = deserializedObjects.get(0);
		if (firstObject instanceof PropertyVO) {
			PropertyListVO propertyVOS = new PropertyListVO();
			deserializedObjects.stream().map(PropertyVO.class::cast).forEach(propertyVOS::add);
			return propertyVOS;
		} else if (firstObject instanceof GeoPropertyVO) {
			GeoPropertyListVO geoPropertyVOS = new GeoPropertyListVO();
			deserializedObjects.stream().map(GeoPropertyVO.class::cast).forEach(geoPropertyVOS::add);
			return geoPropertyVOS;
		} else if (firstObject instanceof RelationshipVO) {
			RelationshipListVO relationshipVOS = new RelationshipListVO();
			deserializedObjects.stream().map(RelationshipVO.class::cast).forEach(relationshipVOS::add);
			return relationshipVOS;
		}
		throw new MappingException("Was not able to deserialize the array.");
	}

}
