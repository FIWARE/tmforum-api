package org.fiware.tmforum.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.PropertyListVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.ngsi.model.RelationshipListVO;
import org.fiware.ngsi.model.RelationshipVO;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Mapper to handle translation from NGSI-LD entities to Java-Objects, based on annotations added to the target class
 */
@Slf4j
@Singleton
public class EntityVOMapper extends Mapper {

    public static final String REALTIONSHIP_OBJECT_KEY = "object";
    public static final String PROPERTY_VALUE_KEY = "value";

    private final ObjectMapper objectMapper;
    private final EntitiesRepository entitiesRepository;

    public EntityVOMapper(ObjectMapper objectMapper, EntitiesRepository entitiesRepository) {
        this.objectMapper = objectMapper;
        this.entitiesRepository = entitiesRepository;
        this.objectMapper
                .addMixIn(AdditionalPropertyVO.class, AdditionalPropertyMixin.class);
    }

    /**
     * Method to map an NGSI-LD Entity into a Java-Object of class targetClass. The class has to provide a string constructor to receive the entity id
     *
     * @param entityVO    the NGSI-LD entity to be mapped
     * @param targetClass class of the target object
     * @param <T>         generic type of the target object, has to extend provide a string-constructor to receive the entity id
     * @return the mapped object
     */
    public <T> Mono<T> fromEntityVO(EntityVO entityVO, Class<T> targetClass) {

        Optional<MappingEnabled> optionalMappingEnabled = isMappingEnabled(targetClass);
        if (!optionalMappingEnabled.isPresent()) {
            return Mono.error(new MappingException(String.format("Mapping is not enabled for class %s", targetClass)));
        }

        MappingEnabled mappingEnabled = optionalMappingEnabled.get();

        if (!Arrays.stream(mappingEnabled.entityType()).toList().contains(entityVO.getType())) {
            return Mono.error(new MappingException(String.format("Entity and Class type do not match - %s vs %s.", entityVO.getType(), Arrays.asList(mappingEnabled.entityType()))));
        }
        return getRelationshipMap(entityVO.getAdditionalProperties(), targetClass)
                .flatMap(relationshipMap -> fromEntityVO(entityVO, targetClass, relationshipMap));

    }

    /**
     * Return a single, emitting the enitites associated with relationships in the given properties maps
     *
     * @param propertiesMap properties map to evaluate
     * @param targetClass   class of the target object
     * @param <T>           the class
     * @return a single, emitting the map of related entities
     */
    private <T> Mono<Map<String, EntityVO>> getRelationshipMap(Map<String, AdditionalPropertyVO> propertiesMap, Class<T> targetClass) {
        return entitiesRepository.getEntities(getRelationshipObjects(propertiesMap, targetClass))
                .map(relationshipsList -> relationshipsList.stream().map(EntityVO.class::cast).collect(Collectors.toMap(e -> e.getId().toString(), e -> e)))
                .defaultIfEmpty(Map.of());
    }

    /**
     * Create the actual object from the entity, after its relations are evaluated.
     *
     * @param entityVO        entity to create the object from
     * @param targetClass     class of the object to be created
     * @param relationShipMap all entities (directly) related to the objects. Sub relationships(e.g. relationships of properties) will be evaluated downstream.
     * @param <T>             the class
     * @return a single, emitting the actual object.
     */
    private <T> Mono<T> fromEntityVO(EntityVO entityVO, Class<T> targetClass, Map<String, EntityVO> relationShipMap) {
        try {
            Constructor<T> objectConstructor = targetClass.getDeclaredConstructor(String.class);
            T constructedObject = objectConstructor.newInstance(entityVO.getId().toString());

            // handle "well-known" properties
            Map<String, AdditionalPropertyVO> propertiesMap = new LinkedHashMap<>();
            propertiesMap.put(EntityVO.JSON_PROPERTY_LOCATION, entityVO.getLocation());
            propertiesMap.put(EntityVO.JSON_PROPERTY_OBSERVATION_SPACE, entityVO.getObservationSpace());
            propertiesMap.put(EntityVO.JSON_PROPERTY_OPERATION_SPACE, entityVO.getOperationSpace());
            propertiesMap.put(EntityVO.JSON_PROPERTY_CREATED_AT, propertyVOFromValue(entityVO.getCreatedAt()));
            propertiesMap.put(EntityVO.JSON_PROPERTY_MODIFIED_AT, propertyVOFromValue(entityVO.getModifiedAt()));
            propertiesMap.putAll(entityVO.getAdditionalProperties());

            List<Mono<T>> singleInvocations = propertiesMap.entrySet().stream()
                    .map(entry -> getObjectInvocation(entry, constructedObject, relationShipMap, entityVO.getId().toString()))
                    .toList();

            return Mono.zip(singleInvocations, constructedObjects -> constructedObject);

        } catch (NoSuchMethodException e) {
            return Mono.error(new MappingException(String.format("The class %s does not declare the required String id constructor.", targetClass)));
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            return Mono.error(new MappingException(String.format("Was not able to create instance of %s.", targetClass), e));
        }
    }

    /**
     * Helper method to create a propertyVO for well-known(thus flat) properties
     *
     * @param value the value to wrap
     * @return a propertyVO containing the value
     */
    private PropertyVO propertyVOFromValue(Object value) {
        PropertyVO propertyVO = new PropertyVO();
        propertyVO.setValue(propertyVO);
        return propertyVO;
    }

    /**
     * Get the invocation on the object to be constructed.
     *
     * @param entry                   additional properties entry
     * @param objectUnderConstruction the new object, to be filled with the values
     * @param relationShipMap         map of preevaluated realtions
     * @param entityId                id of the entity
     * @param <T>                     class of the constructed object
     * @return single, emmiting the constructed object
     */
    private <T> Mono<T> getObjectInvocation(Map.Entry<String, AdditionalPropertyVO> entry, T objectUnderConstruction, Map<String, EntityVO> relationShipMap, String entityId) {
        Optional<Method> optionalSetter = getCorrespondingSetterMethod(objectUnderConstruction, entry.getKey());
        if (optionalSetter.isEmpty()) {
            log.warn("Ignoring property {} for entity {} since there is no mapping configured.", entry.getKey(), entityId);
            return Mono.just(objectUnderConstruction);
        }
        Method setterMethod = optionalSetter.get();
        Optional<AttributeSetter> optionalAttributeSetter = getAttributeSetterAnnotation(setterMethod);
        if (optionalAttributeSetter.isEmpty()) {
            log.warn("Ignoring property {} for entity {} since there is no attribute setter configured.", entry.getKey(), entityId);
            return Mono.just(objectUnderConstruction);
        }
        AttributeSetter setterAnnotation = optionalAttributeSetter.get();

        Class<?> parameterType = getParameterType(setterMethod.getParameterTypes());

        return switch (setterAnnotation.value()) {
            case PROPERTY, GEO_PROPERTY -> handleProperty(entry.getValue(), objectUnderConstruction, optionalSetter.get(), parameterType);
            case PROPERTY_LIST -> handlePropertyList(entry.getValue(), objectUnderConstruction, optionalSetter.get(), setterAnnotation);
            case RELATIONSHIP -> handleRelationship(entry.getValue(), objectUnderConstruction, relationShipMap, optionalSetter.get(), setterAnnotation);
            case RELATIONSHIP_LIST -> handleRelationshipList(entry.getValue(), objectUnderConstruction, relationShipMap, optionalSetter.get(), setterAnnotation);
            default -> Mono.error(new MappingException(String.format("Received type %s is not supported.", setterAnnotation.value())));
        };
    }

    /**
     * Handle the evaluation of a property entry. Returns a single, emitting the target object, while invoking the property setting method.
     *
     * @param propertyValue           the value of the property
     * @param objectUnderConstruction the object under construction
     * @param setter                  the setter to be used for the property
     * @param parameterType           type of the property in the target object
     * @param <T>                     class of the object under construction
     * @return the single, emitting the objectUnderConstruction
     */
    private <T> Mono<T> handleProperty(AdditionalPropertyVO propertyValue, T objectUnderConstruction, Method setter, Class<?> parameterType) {
        if (propertyValue instanceof PropertyVO propertyVO)
            return invokeWithExceptionHandling(setter, objectUnderConstruction, objectMapper.convertValue(propertyVO.getValue(), parameterType));
        else {
            log.error("Mapping exception");
            return Mono.error(new MappingException(String.format("The attribute is not a valid property: %s ", propertyValue)));
        }
    }

    /**
     * Handle the evaluation of a property-list entry. Returns a single, emitting the target object, while invoking the property setting method.
     *
     * @param propertyListObject      the object containing the property-list
     * @param objectUnderConstruction the object under construction
     * @param setter                  the setter to be used for the property
     * @param <T>                     class of the object under construction
     * @return the single, emitting the objectUnderConstruction
     */
    private <T> Mono<T> handlePropertyList(AdditionalPropertyVO propertyListObject, T objectUnderConstruction, Method setter, AttributeSetter setterAnnotation) {
        if (propertyListObject instanceof PropertyListVO propertyVOS) {
            return invokeWithExceptionHandling(setter, objectUnderConstruction, propertyListToTargetClass(propertyVOS, setterAnnotation.targetClass()));
        } else if (propertyListObject instanceof PropertyVO propertyVO) {
            //we need special handling here, since we have no real property lists(see NGSI-LD issue)
            // TODO: remove as soon as ngsi-ld does properly support that.
            if (propertyVO.getValue() instanceof List propertyList) {
                return invokeWithExceptionHandling(setter, objectUnderConstruction, propertyList.stream()
                        .map(listValue -> objectMapper.convertValue(listValue, setterAnnotation.targetClass()))
                        .toList());
            }
            PropertyListVO propertyVOS = new PropertyListVO();
            propertyVOS.add(propertyVO);
            // in case of single element lists, they are returned as a flat property
            return invokeWithExceptionHandling(setter, objectUnderConstruction, propertyListToTargetClass(propertyVOS, setterAnnotation.targetClass()));
        } else {
            return Mono.error(new MappingException(String.format("The attribute is not a valid property list: %v ", propertyListObject)));
        }
    }

    /**
     * Handle the evaluation of a relationship-list entry. Returns a single, emitting the target object, while invoking the property setting method.
     *
     * @param attributeValue          the entry containing the relationship-list
     * @param objectUnderConstruction the object under construction
     * @param relationShipMap         a map containing the pre-evaluated relationships
     * @param setter                  the setter to be used for the property
     * @param setterAnnotation        attribute setter annotation on the method
     * @param <T>                     class of the objectUnderConstruction
     * @return the single, emitting the objectUnderConstruction
     */
    private <T> Mono<T> handleRelationshipList(AdditionalPropertyVO attributeValue, T objectUnderConstruction, Map<String, EntityVO> relationShipMap, Method setter, AttributeSetter setterAnnotation) {
        Class<?> targetClass = setterAnnotation.targetClass();
        if (setterAnnotation.fromProperties()) {
            if (attributeValue instanceof RelationshipVO singlePropertyMap) {
                return relationshipFromProperties(singlePropertyMap, targetClass)
                        .flatMap(relationship -> {
                            // we return the constructed object, since invoke most likely returns null, which is not allowed on mapper functions
                            // a list is created, since we have a relationship-list defined by the annotation
                            return invokeWithExceptionHandling(setter, objectUnderConstruction, List.of(relationship));
                        });
            } else if (attributeValue instanceof RelationshipListVO multiPropertyList) {
                return Mono.zip(multiPropertyList.stream().map(relationshipVO -> relationshipFromProperties(relationshipVO, targetClass)).toList(),
                        oList -> Arrays.asList(oList).stream().map(targetClass::cast).toList()).flatMap(relationshipList -> {
                    // we return the constructed object, since invoke most likely returns null, which is not allowed on mapper functions
                    return invokeWithExceptionHandling(setter, objectUnderConstruction, relationshipList);
                });

            } else {
                return Mono.error(new MappingException(String.format("Value of the relationship %s is invalid.", attributeValue)));
            }
        } else {
            return relationshipListToTargetClass(attributeValue, targetClass, relationShipMap)
                    .flatMap(relatedEntities -> {
                        // we return the constructed object, since invoke most likely returns null, which is not allowed on mapper functions
                        return invokeWithExceptionHandling(setter, objectUnderConstruction, relatedEntities);
                    });
        }
    }

    /**
     * Handle the evaluation of a relationship entry. Returns a single, emitting the target object, while invoking the property setting method.
     *
     * @param relationShip            the object containing the relationship
     * @param objectUnderConstruction the object under construction
     * @param relationShipMap         a map containing the pre-evaluated relationships
     * @param setter                  the setter to be used for the property
     * @param setterAnnotation        attribute setter annotation on the method
     * @param <T>                     class of the objectUnderConstruction
     * @return the single, emitting the objectUnderConstruction
     */
    private <T> Mono<T> handleRelationship(AdditionalPropertyVO relationShip, T objectUnderConstruction, Map<String, EntityVO> relationShipMap, Method setter, AttributeSetter setterAnnotation) {
        Class<?> targetClass = setterAnnotation.targetClass();
        if (relationShip instanceof RelationshipVO relationshipVO) {
            if (setterAnnotation.fromProperties()) {
                return relationshipFromProperties(relationshipVO, targetClass)
                        .flatMap(relatedEntity -> {
                            // we return the constructed object, since invoke most likely returns null, which is not allowed on mapper functions
                            return invokeWithExceptionHandling(setter, objectUnderConstruction, relatedEntity);
                        });
            } else {
                return getObjectFromRelationship(relationshipVO, targetClass, relationShipMap, relationshipVO.getAdditionalProperties())
                        .flatMap(relatedEntity -> {
                            // we return the constructed object, since invoke most likely returns null, which is not allowed on mapper functions
                            return invokeWithExceptionHandling(setter, objectUnderConstruction, relatedEntity);
                        });
                // handle overwrites from property

            }
        } else {
            return Mono.error(new MappingException(String.format("Did not receive a valid relationship: %s", relationShip)));
        }
    }

    /**
     * Invoke the given method and handle potential exceptions.
     */
    private <T> Mono<T> invokeWithExceptionHandling(Method invocationMethod, T objectUnderConstruction, Object... invocationArgs) {
        try {
            invocationMethod.invoke(objectUnderConstruction, invocationArgs);
            return Mono.just(objectUnderConstruction);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return Mono.error(e);
        } catch (RuntimeException e) {
            return Mono.error(e);
        }
    }

    /**
     * Create the target object of a relationship from its properties(instead of entities additionally retrieved)
     *
     * @param relationshipVO representation of the current relationship(as provided by the original entitiy)
     * @param targetClass    class of the target object to be created(e.g. the object representing the relationship)
     * @param <T>            the class
     * @return a single emitting the object representing the relationship
     */
    private <T> Mono<T> relationshipFromProperties(RelationshipVO relationshipVO, Class<T> targetClass) {
        try {
            String entityID = relationshipVO.getObject().toString();

            Constructor<T> objectConstructor = targetClass.getDeclaredConstructor(String.class);
            T constructedObject = objectConstructor.newInstance(entityID);

            Map<String, Method> attributeSetters = getAttributeSetterMethodMap(constructedObject);

            return Mono.zip(attributeSetters.entrySet().stream()
                    .map(methodEntry -> {
                        String field = methodEntry.getKey();
                        Method setterMethod = methodEntry.getValue();
                        Optional<AttributeSetter> optionalAttributeSetterAnnotation = getAttributeSetterAnnotation(setterMethod);
                        if (optionalAttributeSetterAnnotation.isEmpty()) {
                            // no setter for the field, can be ignored
                            log.debug("No setter defined for field {}", field);
                            return Mono.just(constructedObject);
                        }
                        AttributeSetter setterAnnotation = optionalAttributeSetterAnnotation.get();

                        Optional<AdditionalPropertyVO> optionalProperty = switch (methodEntry.getKey()) {
                            case RelationshipVO.JSON_PROPERTY_OBSERVED_AT -> Optional.ofNullable(relationshipVO.getObservedAt()).map(this::propertyVOFromValue);
                            case RelationshipVO.JSON_PROPERTY_CREATED_AT -> Optional.ofNullable(relationshipVO.getCreatedAt()).map(this::propertyVOFromValue);
                            case RelationshipVO.JSON_PROPERTY_MODIFIED_AT -> Optional.ofNullable(relationshipVO.getModifiedAt()).map(this::propertyVOFromValue);
                            case RelationshipVO.JSON_PROPERTY_DATASET_ID -> Optional.ofNullable(relationshipVO.getDatasetId()).map(this::propertyVOFromValue);
                            case RelationshipVO.JSON_PROPERTY_INSTANCE_ID -> Optional.ofNullable(relationshipVO.getInstanceId()).map(this::propertyVOFromValue);
                            default -> Optional.empty();
                        };

                        // try to find the attribute from the additional properties
                        if (optionalProperty.isEmpty() && relationshipVO.getAdditionalProperties() != null && relationshipVO.getAdditionalProperties().containsKey(field)) {
                            optionalProperty = Optional.ofNullable(relationshipVO.getAdditionalProperties().get(field));
                        }

                        return optionalProperty.map(attributeValue -> {
                            return switch (setterAnnotation.value()) {
                                case PROPERTY, GEO_PROPERTY -> handleProperty(attributeValue, constructedObject, setterMethod, setterAnnotation.targetClass());
                                case RELATIONSHIP -> getRelationshipMap(relationshipVO.getAdditionalProperties(), targetClass)
                                        .map(rm -> handleRelationship(attributeValue, constructedObject, rm, setterMethod, setterAnnotation)); //resolve objects;
                                case RELATIONSHIP_LIST -> getRelationshipMap(relationshipVO.getAdditionalProperties(), targetClass)
                                        .map(rm -> handleRelationshipList(attributeValue, constructedObject, rm, setterMethod, setterAnnotation));
                                case PROPERTY_LIST -> handlePropertyList(attributeValue, constructedObject, setterMethod, setterAnnotation);
                                default -> Mono.error(new MappingException(String.format("Received type %s is not supported.", setterAnnotation.value())));
                            };
                        }).orElse(Mono.just(constructedObject));

                    }).toList(), constructedObjects -> constructedObject);

        } catch (NoSuchMethodException e) {
            return Mono.error(new MappingException(String.format("The class %s does not declare the required String id constructor.", targetClass)));
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            return Mono.error(new MappingException(String.format("Was not able to create instance of %s.", targetClass), e));
        }
    }

    /**
     * Returns a list of all entityIDs that are defined as relationships from the given entity.
     *
     * @param additionalProperties map of the properties to evaluate
     * @param targetClass          target class of the mapping
     * @param <T>                  the class
     * @return a list of uris
     */
    private <T> List<URI> getRelationshipObjects(Map<String, AdditionalPropertyVO> additionalProperties, Class<T> targetClass) {
        return Arrays.stream(targetClass.getMethods())
                .map(this::getAttributeSetterAnnotation)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(a -> (a.value().equals(AttributeType.RELATIONSHIP) || a.value().equals(AttributeType.RELATIONSHIP_LIST)))
                // we don't need to retrieve entities that should be filled from the properties.
                .filter(a -> !a.fromProperties())
                .flatMap(attributeSetter -> getEntityURIsByAttributeSetter(attributeSetter, additionalProperties).stream())
                .toList();

    }

    /**
     * Evaluate a properties map to get all referenced entity ids
     *
     * @param attributeSetter the attribute setter annotation
     * @param propertiesMap   the properties map to check
     * @return a list of entity ids
     */
    private List<URI> getEntityURIsByAttributeSetter(AttributeSetter attributeSetter, Map<String, AdditionalPropertyVO> propertiesMap) {
        return Optional.ofNullable(propertiesMap.get(attributeSetter.targetName()))
                .map(this::getURIsFromRelationshipObject)
                .orElseGet(List::of);
    }

    /**
     * Evaluate a concrete object of a realitonship. If its a list of objects, get the ids of all entities.
     *
     * @param additionalPropertyVO the object to evaluate
     * @return a list of all referenced ids
     */
    private List<URI> getURIsFromRelationshipObject(AdditionalPropertyVO additionalPropertyVO) {
        if (additionalPropertyVO instanceof RelationshipVO relationshipVO) {
            // List.of() cannot be used, since we need a mutable list
            List<URI> uriList = new ArrayList<>();
            uriList.add(relationshipVO.getObject());
            return uriList;
        } else if (additionalPropertyVO instanceof RelationshipListVO relationshipList) {
            return relationshipList.stream().flatMap(listEntry -> getURIsFromRelationshipObject(listEntry).stream()).toList();
        }
        return List.of();
    }

    /**
     * Method to translate a Map-Entry(e.g. NGSI-LD relationship) to a typed list as defined by the target object
     *
     * @param entry       attribute of the entity, e.g. a relationship or a list of relationships
     * @param targetClass class to be used as type for the typed list
     * @param <T>         the type
     * @return a list of objects, mapping the relationship
     */
    private <T> Mono<List<T>> relationshipListToTargetClass(AdditionalPropertyVO entry, Class<T> targetClass, Map<String, EntityVO> relationShipEntitiesMap) {
        if (entry instanceof RelationshipVO relationshipVO) {
            return getObjectFromRelationship(relationshipVO, targetClass, relationShipEntitiesMap, relationshipVO.getAdditionalProperties()).map(List::of);
        } else if (entry instanceof RelationshipListVO relationshipMap) {
            return zipToList(relationshipMap.stream(), targetClass, relationShipEntitiesMap);

        }
        return Mono.error(new MappingException(String.format("Did not receive a valid entry: %s", entry)));
    }

    /**
     * Helper method for combining the evaluation of relationship entities to a single result lits
     *
     * @param relationshipVOStream    the relationships to evaluate
     * @param targetClass             target class of the relationship object
     * @param relationShipEntitiesMap map of the preevaluated relationship entities
     * @param <T>                     target class of the relationship
     * @return a single emitting the full list
     */
    private <T> Mono<List<T>> zipToList(Stream<RelationshipVO> relationshipVOStream, Class<T> targetClass, Map<String, EntityVO> relationShipEntitiesMap) {
        return Mono.zip(
                relationshipVOStream.map(RelationshipVO::getObject)
                        .filter(Objects::nonNull)
                        .map(URI::toString)
                        .map(relationShipEntitiesMap::get)
                        .map(entity -> fromEntityVO(entity, targetClass))
                        .toList(),
                oList -> Arrays.stream(oList).map(targetClass::cast).toList()
        );
    }

    /**
     * Method to translate a Map-Entry(e.g. NGSI-LD property) to a typed list as defined by the target object
     *
     * @param propertyVOS a list of properties
     * @param targetClass class to be used as type for the typed list
     * @param <T>         the type
     * @return a list of objects, mapping the relationship
     */
    private <T> List<T> propertyListToTargetClass(PropertyListVO propertyVOS, Class<T> targetClass) {
        return propertyVOS.stream().map(propertyEntry -> objectMapper.convertValue(propertyEntry.getValue(), targetClass)).toList();
    }

    /**
     * Retrieve the object from a relationship and return it as a java object of class T. All sub relationships will be evaluated, too.
     *
     * @param relationshipVO the relationship entry
     * @param targetClass    the target-class of the entry
     * @param <T>            the class
     * @return the actual object
     */
    private <T> Mono<T> getObjectFromRelationship(RelationshipVO relationshipVO, Class<T> targetClass, Map<String, EntityVO> relationShipEntitiesMap, Map<String, AdditionalPropertyVO> additionalPropertyVOMap) {
        EntityVO entityVO = relationShipEntitiesMap.get(relationshipVO.getObject().toString());
        //merge with override properties
        if (additionalPropertyVOMap != null) {
            entityVO.getAdditionalProperties().putAll(additionalPropertyVOMap);
        }
        return fromEntityVO(entityVO, targetClass);

    }

    /**
     * Return the type of the setter's parameter.
     */
    private Class<?> getParameterType(Class<?>[] arrayOfClasses) {
        if (arrayOfClasses.length != 1) {
            throw new MappingException("Setter method should only have one parameter declared.");
        }
        return arrayOfClasses[0];
    }

    /**
     * Get the setter method for the given property at the entity.
     */
    private <T> Optional<Method> getCorrespondingSetterMethod(T entity, String propertyName) {
        return getAttributeSettersMethods(entity).stream().filter(m ->
                        getAttributeSetterAnnotation(m)
                                .map(attributeSetter -> attributeSetter.targetName().equals(propertyName)).orElse(false))
                .findFirst();
    }

    /**
     * Get all attribute setters for the given entity
     */
    private <T> List<Method> getAttributeSettersMethods(T entity) {
        return Arrays.stream(entity.getClass().getMethods()).filter(m -> getAttributeSetterAnnotation(m).isPresent()).toList();
    }

    private <T> Map<String, Method> getAttributeSetterMethodMap(T entity) {
        return Arrays.stream(entity.getClass().getMethods())
                .filter(m -> getAttributeSetterAnnotation(m).isPresent())
                .collect(Collectors.toMap(m -> getAttributeSetterAnnotation(m).get().targetName(), m -> m));
    }

    /**
     * Get the attribute setter annotation from the given method, if it exists.
     */
    private Optional<AttributeSetter> getAttributeSetterAnnotation(Method m) {
        return Arrays.stream(m.getAnnotations()).filter(AttributeSetter.class::isInstance)
                .findFirst()
                .map(AttributeSetter.class::cast);
    }

}
