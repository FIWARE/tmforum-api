package org.fiware.tmforum.mapping;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeResolver;

/**
 * Since the list objects(PropertyList, GeoPropertyList, RelationshipList) cannot be uniquely deduced, we need to mixin our custom resolve.
 * The normal jackson resolvers use type information provided by field of the object or fieldnames of the object or the overall signature.
 * Our lists are dependent on a field value(e.g. type) of the list objects, which is not supported by the default resolvers.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonTypeResolver(AdditionalPropertyTypeResolver.class)
public abstract class AdditionalPropertyMixin {
}
