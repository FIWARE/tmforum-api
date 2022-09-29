package org.fiware.tmforum.mapping;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.TypeNameIdResolver;

import java.util.Collection;

/**
 * Resolver for our additional properties, to be used for deserialization.
 * It can deduce Property, GeoProperty, Relationship, PropertyList, GeoPropertyList and RelationShipList.
 * There is no need for a custom serializer, since the serializer has a concrete object at hand and the type is already
 * model immanent.
 */
public class AdditionalPropertyTypeResolver extends StdTypeResolverBuilder {

	public static final String NGSI_LD_TYPE_PROPERTY_NAME = "type";

	@Override
	public TypeDeserializer buildTypeDeserializer(DeserializationConfig config, JavaType baseType, Collection<NamedType> subtypes) {
		return new AdditionalPropertyDeserializer(
				baseType,
				// normal type-to-name resolver is sufficient
				TypeNameIdResolver.construct(config, baseType, subtypes, false, true),
				// per ngsi-ld, the type is in the type-property
				NGSI_LD_TYPE_PROPERTY_NAME,
				false,
				// hopefully never needed, but default behaviour is sufficient
				defineDefaultImpl(config, baseType));
	}


}
