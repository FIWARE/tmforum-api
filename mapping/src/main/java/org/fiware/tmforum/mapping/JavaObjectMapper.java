package org.fiware.tmforum.mapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.GeoPropertyVO;
import org.fiware.ngsi.model.PropertyListVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipListVO;
import org.fiware.ngsi.model.RelationshipVO;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.DatasetId;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.RelationshipObject;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mapper to handle translation from Java-Objects into NGSI-LD entities.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class JavaObjectMapper extends Mapper {

    private static final String DEFAULT_CONTEXT = "https://smartdatamodels.org/context.jsonld";

    public static final String NO_MAPPING_DEFINED_FOR_METHOD_TEMPLATE = "No mapping defined for method %s";
    public static final String WAS_NOT_ABLE_INVOKE_METHOD_TEMPLATE = "Was not able invoke method %s on %s";

    /**
     * Translate the given object into an Entity.
     *
     * @param entity the object representing the entity
     * @param <T>    class of the entity
     * @return the NGIS-LD entity objet
     */
    public <T> EntityVO toEntityVO(T entity) {
        isMappingEnabled(entity.getClass())
                .orElseThrow(() -> new UnsupportedOperationException(String.format("Generic mapping to NGSI-LD entities is not supported for object %s", entity)));

        List<Method> entityIdMethod = new ArrayList<>();
        List<Method> entityTypeMethod = new ArrayList<>();
        List<Method> propertyMethods = new ArrayList<>();
        List<Method> propertyListMethods = new ArrayList<>();
        List<Method> relationshipMethods = new ArrayList<>();
        List<Method> relationshipListMethods = new ArrayList<>();
        List<Method> geoPropertyMethods = new ArrayList<>();
        List<Method> geoPropertyListMethods = new ArrayList<>();

        Arrays.stream(entity.getClass().getMethods()).forEach(method -> {
            if (isEntityIdMethod(method)) {
                entityIdMethod.add(method);
            } else if (isEntityTypeMethod(method)) {
                entityTypeMethod.add(method);
            } else {
                getAttributeGetter(method.getAnnotations()).ifPresent(annotation -> {
                    switch (annotation.value()) {
                        case PROPERTY -> propertyMethods.add(method);
                        // We handle property lists the same way as properties, since it is mapped as a property which value is a json array.
                        // A real NGSI-LD property list would require a datasetId, that is not provided here.
                        case PROPERTY_LIST -> propertyMethods.add(method);
                        case GEO_PROPERTY -> geoPropertyMethods.add(method);
                        case RELATIONSHIP -> relationshipMethods.add(method);
                        case GEO_PROPERTY_LIST -> geoPropertyListMethods.add(method);
                        case RELATIONSHIP_LIST -> relationshipListMethods.add(method);
                        default -> throw new UnsupportedOperationException(String.format("Mapping target %s is not supported.", annotation.value()));
                    }
                });
            }
        });

        if (entityIdMethod.size() != 1) {
            throw new IllegalArgumentException(String.format("The provided object declares %s id methods, exactly one is expected.", entityIdMethod.size()));
        }
        if (entityTypeMethod.size() != 1) {
            throw new IllegalArgumentException(String.format("The provided object declares %s type methods, exactly one is expected.", entityTypeMethod.size()));

        }

        return buildEntity(entity, entityIdMethod.get(0), entityTypeMethod.get(0), propertyMethods, propertyListMethods, geoPropertyMethods, relationshipMethods, relationshipListMethods);
    }

    /**
     * Build the entity from its declared methods.
     */
    private <T> EntityVO buildEntity(T entity, Method entityIdMethod, Method entityTypeMethod, List<Method> propertyMethods, List<Method> propertyListMethods, List<Method> geoPropertyMethods, List<Method> relationshipMethods, List<Method> relationshipListMethods) {

        EntityVO entityVO = new EntityVO();
        // TODO: Check if we need that configurable
        entityVO.setAtContext(DEFAULT_CONTEXT);

        // TODO: include extraction via annotation for all well-known attributes
        entityVO.setOperationSpace(null);
        entityVO.setObservationSpace(null);
        entityVO.setLocation(null);

        try {
            Object entityIdObject = entityIdMethod.invoke(entity);
            if (!(entityIdObject instanceof URI)) {
                throw new IllegalArgumentException(String.format("The entityId method does not return a valid URI for entity %s.", entity));
            }
            entityVO.id((URI) entityIdObject);

            Object entityTypeObject = entityTypeMethod.invoke(entity);
            if (!(entityTypeObject instanceof String)) {
                throw new IllegalArgumentException("The entityType method does not return a valid String.");
            }
            entityVO.setType((String) entityTypeObject);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(String.format(WAS_NOT_ABLE_INVOKE_METHOD_TEMPLATE, "unknown-method", entity), e);
        }

        Map<String, AdditionalPropertyVO> additionalProperties = new LinkedHashMap<>();
        additionalProperties.putAll(buildProperties(entity, propertyMethods));
        additionalProperties.putAll(buildPropertyList(entity, propertyListMethods));
        additionalProperties.putAll(buildGeoProperties(entity, geoPropertyMethods));
        Map<String, RelationshipVO> relationshipVOMap = buildRelationships(entity, relationshipMethods);
        Map<String, RelationshipListVO> relationshipListVOMap = buildRelationshipList(entity, relationshipListMethods);
        // we need to post-process the relationships, since orion-ld only accepts dataset-ids for lists > 1
        relationshipVOMap.entrySet().stream().forEach(e -> e.getValue().setDatasetId(null));
        relationshipListVOMap.entrySet().stream().forEach(e -> {
            if (e.getValue().size() == 1) {
                e.getValue().get(0).setDatasetId(null);
            }
        });

        additionalProperties.putAll(relationshipVOMap);
        additionalProperties.putAll(relationshipListVOMap);

        additionalProperties.forEach(entityVO::setAdditionalProperties);

        return entityVO;
    }

    /**
     * Check if the given method defines the entity type
     */
    private boolean isEntityTypeMethod(Method method) {
        return Arrays.stream(method.getAnnotations()).anyMatch(EntityType.class::isInstance);
    }

    /**
     * Check if the given method defines the entity id
     */
    private boolean isEntityIdMethod(Method method) {
        return Arrays.stream(method.getAnnotations()).anyMatch(EntityId.class::isInstance);
    }

    /**
     * Build a relationship from the declared methods
     */
    private <T> Map<String, RelationshipVO> buildRelationships(T entity, List<Method> relationshipMethods) {
        return relationshipMethods.stream()
                .map(method -> methodToRelationshipEntry(entity, method))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Build a list of relationships from the declared methods
     */
    private <T> Map<String, RelationshipListVO> buildRelationshipList(T entity, List<Method> relationshipListMethods) {
        return relationshipListMethods.stream()
                .map(relationshipMethod -> methodToRelationshipListEntry(entity, relationshipMethod))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /*
     * Build a list of properties from the declared methods
     */
    private <T> Map<String, PropertyListVO> buildPropertyList(T entity, List<Method> propertyListMethods) {
        return propertyListMethods.stream()
                .map(propertyListMethod -> methodToPropertyListEntry(entity, propertyListMethod))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Build geoproperties from the declared methods
     */
    private <T> Map<String, GeoPropertyVO> buildGeoProperties(T entity, List<Method> geoPropertyMethods) {
        return geoPropertyMethods.stream()
                .map(geoPropertyMethod -> methodToGeoPropertyEntry(entity, geoPropertyMethod))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Build properties from the declared methods
     */
    private <T> Map<String, PropertyVO> buildProperties(T entity, List<Method> propertyMethods) {
        return propertyMethods.stream()
                .map(propertyMethod -> methodToPropertyEntry(entity, propertyMethod))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Return method defining the object of the relationship for the given entity, if exists.
     */
    private <T> Optional<Method> getRelationshipObjectMethod(T entity) {
        return Arrays.stream(entity.getClass().getMethods()).filter(this::isRelationShipObject).findFirst();
    }

    /**
     * Return method defining the datasetid for the given entity, if exists.
     */
    private <T> Optional<Method> getDatasetIdMethod(T entity) {
        return Arrays.stream(entity.getClass().getMethods()).filter(this::isDatasetId).findFirst();
    }

    /**
     * Get all methods declared as attribute getters.
     */
    private <T> List<Method> getAttributeGettersMethods(T entity) {
        return Arrays.stream(entity.getClass().getMethods()).filter(m -> getAttributeGetterAnnotation(m).isPresent()).toList();
    }

    /**
     * return the {@link  AttributeGetter} annotation for the method if there is such.
     */
    private Optional<AttributeGetter> getAttributeGetterAnnotation(Method m) {
        return Arrays.stream(m.getAnnotations()).filter(AttributeGetter.class::isInstance).findFirst().map(AttributeGetter.class::cast);
    }

    /**
     * Find the attribute getter from all the annotations.
     */
    private Optional<AttributeGetter> getAttributeGetter(Annotation[] annotations) {
        return Arrays.stream(annotations).filter(AttributeGetter.class::isInstance).map(AttributeGetter.class::cast).findFirst();
    }


    /**
     * Check if the given method is declared to be used as object of a relationship
     */
    private boolean isRelationShipObject(Method m) {
        return Arrays.stream(m.getAnnotations()).anyMatch(RelationshipObject.class::isInstance);
    }

    /**
     * Check if the given method is declared to be used as datasetId
     */
    private boolean isDatasetId(Method m) {
        return Arrays.stream(m.getAnnotations()).anyMatch(DatasetId.class::isInstance);
    }

    /**
     * Build a property entry from the given method on the entity
     */
    private <T> Optional<Map.Entry<String, PropertyVO>> methodToPropertyEntry(T entity, Method method) {
        try {
            Object propertyObject = method.invoke(entity);
            if (propertyObject == null) {
                return Optional.empty();
            }
            AttributeGetter attributeMapping = getAttributeGetter(method.getAnnotations()).orElseThrow(() -> new IllegalArgumentException(String.format(NO_MAPPING_DEFINED_FOR_METHOD_TEMPLATE, method)));

            PropertyVO propertyVO = new PropertyVO();
            propertyVO.setValue(propertyObject);

            return Optional.of(new AbstractMap.SimpleEntry<>(attributeMapping.targetName(), propertyVO));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(String.format(WAS_NOT_ABLE_INVOKE_METHOD_TEMPLATE, method, entity));
        }
    }

    /**
     * Build a geo-property entry from the given method on the entity
     */
    private <T> Optional<Map.Entry<String, GeoPropertyVO>> methodToGeoPropertyEntry(T entity, Method method) {
        try {
            Object o = method.invoke(entity);
            if (o == null) {
                return Optional.empty();
            }
            AttributeGetter attributeMapping = getAttributeGetter(method.getAnnotations()).orElseThrow(() -> new IllegalArgumentException(String.format(NO_MAPPING_DEFINED_FOR_METHOD_TEMPLATE, method)));
            GeoPropertyVO geoPropertyVO = new GeoPropertyVO();
            geoPropertyVO.setValue(o);
            return Optional.of(new AbstractMap.SimpleEntry<>(attributeMapping.targetName(), geoPropertyVO));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(String.format(WAS_NOT_ABLE_INVOKE_METHOD_TEMPLATE, method, entity));
        }
    }

    /**
     * Build a relationship entry from the given method on the entity
     */
    private <T> Optional<Map.Entry<String, RelationshipVO>> methodToRelationshipEntry(T entity, Method method) {
        try {
            Object relationShipObject = method.invoke(entity);
            if (relationShipObject == null) {
                return Optional.empty();
            }
            RelationshipVO relationshipVO = getRelationshipVO(method, relationShipObject);
            AttributeGetter attributeMapping = getAttributeGetter(method.getAnnotations()).orElseThrow(() -> new IllegalArgumentException(String.format(NO_MAPPING_DEFINED_FOR_METHOD_TEMPLATE, method)));
            return Optional.of(new AbstractMap.SimpleEntry<>(attributeMapping.targetName(), relationshipVO));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new MappingException(String.format(WAS_NOT_ABLE_INVOKE_METHOD_TEMPLATE, method, entity));
        }
    }

    /**
     * Build a relationship list entry from the given method on the entity
     */
    private <T> Optional<Map.Entry<String, RelationshipListVO>> methodToRelationshipListEntry(T entity, Method method) {
        try {
            Object o = method.invoke(entity);
            if (o == null) {
                return Optional.empty();
            }
            if (!(o instanceof List)) {
                throw new IllegalArgumentException(String.format("Relationship list method %s::%s did not return a List.", entity, method));
            }
            List<Object> entityObjects = (List) o;

            AttributeGetter attributeGetter = getAttributeGetter(method.getAnnotations()).orElseThrow(() -> new IllegalArgumentException(String.format(NO_MAPPING_DEFINED_FOR_METHOD_TEMPLATE, method)));
            RelationshipListVO relationshipVOS = new RelationshipListVO();
            entityObjects.stream().forEach(eO -> {
                if (eO == null) {
                    throw new MappingException("Null objects inside the relationship list are not allowed");
                }
            });
            relationshipVOS.addAll(entityObjects.stream()
                    .map(entityObject -> getRelationshipVO(method, entityObject))
                    .toList());
            return Optional.of(new AbstractMap.SimpleEntry<>(attributeGetter.targetName(), relationshipVOS));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(String.format(WAS_NOT_ABLE_INVOKE_METHOD_TEMPLATE, method, entity));
        }
    }

    /**
     * Get the relationship for the given method and relationship object
     */
    private RelationshipVO getRelationshipVO(Method method, Object relationShipObject) {
        try {

            Method objectMethod = getRelationshipObjectMethod(relationShipObject).orElseThrow(() -> new MappingException(String.format("The relationship %s-%s does not provide an object method.", relationShipObject, method)));
            Object objectObject = objectMethod.invoke(relationShipObject);
            if (!(objectObject instanceof URI)) {
                throw new MappingException(String.format("The object %s of the relationship is not a URI.", relationShipObject));
            }

            Method datasetIdMethod = getDatasetIdMethod(relationShipObject).orElseThrow(() -> new MappingException(String.format("The relationship %s-%s does not provide a datasetId method.", relationShipObject, method)));
            Object datasetIdObject = datasetIdMethod.invoke(relationShipObject);
            if (!(datasetIdObject instanceof URI)) {
                throw new MappingException(String.format("The datasetId %s of the relationship is not a URI.", relationShipObject));
            }
            RelationshipVO relationshipVO = new RelationshipVO();
            relationshipVO.setObject((URI) objectObject);
            relationshipVO.setDatasetId((URI) datasetIdObject);

            // get additional properties. We do not support more depth/complexity for now
            Map<String, AdditionalPropertyVO> additionalProperties = getAttributeGettersMethods(relationShipObject).stream()
                    .map(getterMethod -> getAdditionalPropertyEntryFromMethod(relationShipObject, getterMethod))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            additionalProperties.forEach(relationshipVO::setAdditionalProperties);

            return relationshipVO;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new MappingException(String.format(WAS_NOT_ABLE_INVOKE_METHOD_TEMPLATE, method, relationShipObject));
        }
    }

    /**
     * Get all additional properties for the object of the relationship
     */
    private Optional<Map.Entry<String, PropertyVO>> getAdditionalPropertyEntryFromMethod(Object relationShipObject, Method getterMethod) {
        Optional<AttributeGetter> optionalAttributeGetter = getAttributeGetter(getterMethod.getAnnotations());
        if (optionalAttributeGetter.isEmpty() || !optionalAttributeGetter.get().embedProperty()) {
            return Optional.empty();
        }
        if (optionalAttributeGetter.get().value().equals(AttributeType.PROPERTY)) {
            return methodToPropertyEntry(relationShipObject, getterMethod);
        } else {
            return Optional.empty();
        }
    }


    /**
     * Build a property list entry from the given method on the entity
     */
    private <T> Optional<Map.Entry<String, PropertyListVO>> methodToPropertyListEntry(T entity, Method method) {
        try {
            Object o = method.invoke(entity);
            if (o == null) {
                return Optional.empty();
            }
            if (!(o instanceof List)) {
                throw new IllegalArgumentException(String.format("Property list method %s::%s did not return a List.", entity, method));
            }
            AttributeGetter attributeMapping = getAttributeGetter(method.getAnnotations()).orElseThrow(() -> new IllegalArgumentException(String.format(NO_MAPPING_DEFINED_FOR_METHOD_TEMPLATE, method)));
            List<Object> entityObjects = (List) o;

            PropertyListVO propertyVOS = new PropertyListVO();

            propertyVOS.addAll(entityObjects.stream()
                    .map(propertyObject -> {
                        PropertyVO propertyVO = new PropertyVO();
                        propertyVO.setValue(propertyObject);
                        return propertyVO;
                    })
                    .toList());

            return Optional.of(new AbstractMap.SimpleEntry<>(attributeMapping.targetName(), propertyVOS));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(String.format(WAS_NOT_ABLE_INVOKE_METHOD_TEMPLATE, method, entity));
        }
    }

}


