package org.fiware.tmforum.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
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
@RequiredArgsConstructor
public class EntityVOMapper extends Mapper {

	public static final String REALTIONSHIP_OBJECT_KEY = "object";
	public static final String PROPERTY_VALUE_KEY = "value";

	private final ObjectMapper objectMapper;
	private final EntitiesRepository entitiesRepository;

	/**
	 * Method to map an NGSI-LD Entity into a Java-Object of class targetClass. The class has to provide a string constructor to receive the entity id
	 *
	 * @param entityVO    the NGSI-LD entity to be mapped
	 * @param targetClass class of the target object
	 * @param <T>         generic type of the target object, has to extend provide a string-constructor to receive the entity id
	 * @return the mapped object
	 */
	public <T> Single<T> fromEntityVO(EntityVO entityVO, Class<T> targetClass) {

		MappingEnabled mappingEnabled = isMappingEnabled(targetClass).orElseThrow(() -> new MappingException(String.format("Mapping is not enabled for class %s", targetClass)));

		if (!Arrays.stream(mappingEnabled.entityType()).toList().contains(entityVO.getType())) {
			throw new MappingException(String.format("Entity and Class type do not match - %s vs %s.", entityVO.getType(), Arrays.asList(mappingEnabled.entityType())));
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
	private <T> Single<Map<String, EntityVO>> getRelationshipMap(Map<String, Object> propertiesMap, Class<T> targetClass) {
		return entitiesRepository.getEntities(getRelationshipObjects(propertiesMap, targetClass))
				.map(relationshipsList -> relationshipsList.stream().collect(Collectors.toMap(e -> e.getId().toString(), e -> e)));
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
	private <T> Single<T> fromEntityVO(EntityVO entityVO, Class<T> targetClass, Map<String, EntityVO> relationShipMap) {
		try {
			Constructor<T> objectConstructor = targetClass.getDeclaredConstructor(String.class);
			T constructedObject = objectConstructor.newInstance(entityVO.getId().toString());

			List<Single<T>> singleInvocations = entityVO.getAdditionalProperties().entrySet().stream()
					.map(entry -> getObjectInvocation(entry, constructedObject, relationShipMap, entityVO.id().toString()))
					.toList();

			return Single.zip(singleInvocations, constructedObjects -> constructedObject);

		} catch (NoSuchMethodException e) {
			throw new MappingException(String.format("The class %s does not declare the required String id constructor.", targetClass));
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new MappingException(String.format("Was not able to create instance of %s.", targetClass), e);
		}
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
	private <T> Single<T> getObjectInvocation(Map.Entry<String, Object> entry, T objectUnderConstruction, Map<String, EntityVO> relationShipMap, String entityId) {
		Optional<Method> optionalSetter = getCorrespondingSetterMethod(objectUnderConstruction, entry.getKey());
		if (optionalSetter.isEmpty()) {
			log.warn("Ignoring property {} for entity {} since there is no mapping configured.", entry.getKey(), entityId);
			return Single.just(objectUnderConstruction);
		}
		Method setterMethod = optionalSetter.get();
		Optional<AttributeSetter> optionalAttributeSetter = getAttributeSetterAnnotation(setterMethod);
		if (optionalAttributeSetter.isEmpty()) {
			log.warn("Ignoring property {} for entity {} since there is no attribute setter configured.", entry.getKey(), entityId);
			return Single.just(objectUnderConstruction);
		}
		AttributeSetter setterAnnotation = optionalAttributeSetter.get();

		Class<?> parameterType = getParameterType(setterMethod.getParameterTypes());

		return switch (setterAnnotation.value()) {
			case PROPERTY, GEO_PROPERTY -> handleProperty(entry, objectUnderConstruction, optionalSetter.get(), parameterType);
			case PROPERTY_LIST -> handlePropertyList(entry, objectUnderConstruction, optionalSetter.get(), setterAnnotation);
			case RELATIONSHIP -> handleRelationship(entry, objectUnderConstruction, relationShipMap, optionalSetter.get(), setterAnnotation);
			case RELATIONSHIP_LIST -> handleRelationshipList(entry, objectUnderConstruction, relationShipMap, optionalSetter.get(), setterAnnotation);
			default -> throw new MappingException(String.format("Received type %s is not supported.", setterAnnotation.value()));
		};
	}

	/**
	 * Handle the evaluation of a property entry. Returns a single, emitting the target object, while invoking the property setting method.
	 *
	 * @param entry                   the entry containing the property
	 * @param objectUnderConstruction the object under construction
	 * @param setter                  the setter to be used for the property
	 * @param parameterType           type of the property in the target object
	 * @param <T>                     class of the object under construction
	 * @return the single, emitting the objectUnderConstruction
	 */
	private <T> Single<T> handleProperty(Map.Entry<String, Object> entry, T objectUnderConstruction, Method setter, Class<?> parameterType) {
		if (entry.getValue() instanceof Map propertyMap) {
			return Single.fromCallable(() -> {
				setter.invoke(objectUnderConstruction, objectMapper.convertValue(propertyMap.get(PROPERTY_VALUE_KEY), parameterType));
				return objectUnderConstruction;
			});
		} else {
			throw new MappingException(String.format("Value of the property %s was not a map.", entry));
		}
	}

	/**
	 * Handle the evaluation of a property-list entry. Returns a single, emitting the target object, while invoking the property setting method.
	 *
	 * @param entry                   the entry containing the property-list
	 * @param objectUnderConstruction the object under construction
	 * @param setter                  the setter to be used for the property
	 * @param <T>                     class of the object under construction
	 * @return the single, emitting the objectUnderConstruction
	 */
	private <T> Single<T> handlePropertyList(Map.Entry<String, Object> entry, T objectUnderConstruction, Method setter, AttributeSetter setterAnnotation) {
		Class<?> targetClassMapping = setterAnnotation.targetClass();
		return Single.fromCallable(() -> {
			setter.invoke(objectUnderConstruction, propertyListToTargetClass(entry, targetClassMapping));
			return objectUnderConstruction;
		});
	}

	/**
	 * Handle the evaluation of a relationship-list entry. Returns a single, emitting the target object, while invoking the property setting method.
	 *
	 * @param entry                   the entry containing the relationship-list
	 * @param objectUnderConstruction the object under construction
	 * @param relationShipMap         a map containing the pre-evaluated relationships
	 * @param setter                  the setter to be used for the property
	 * @param setterAnnotation        attribute setter annotation on the method
	 * @param <T>                     class of the objectUnderConstruction
	 * @return the single, emitting the objectUnderConstruction
	 */
	private <T> Single<T> handleRelationshipList(Map.Entry<String, Object> entry, T objectUnderConstruction, Map<String, EntityVO> relationShipMap, Method setter, AttributeSetter setterAnnotation) {
		Class<?> targetClass = setterAnnotation.targetClass();
		if (setterAnnotation.fromProperties()) {
			if (entry.getValue() instanceof Map singlePropertyMap) {
				return relationshipFromProperties(singlePropertyMap, targetClass)
						.map(relationship -> {
							// we return the constructed object, since invoke most likely returns null, which is not allowed on mapper functions
							// a list is created, since we have a relationship-list defined by the annotation
							setter.invoke(objectUnderConstruction, List.of(relationship));
							return objectUnderConstruction;
						});
			} else if (entry.getValue() instanceof List multiPropertyList) {
				return Single.zip(multiPropertyList.stream().map(property -> {
							if (entry.getValue() instanceof Map relationshipRepresentation) {
								return relationshipFromProperties(relationshipRepresentation, targetClass);
							}
							throw new MappingException(String.format("Did not receive a valid relationship: %s", entry));
						}).toList(),
						oList -> Arrays.asList(oList).stream().map(targetClass::cast).toList()).map(relationshipList -> {
					// we return the constructed object, since invoke most likely returns null, which is not allowed on mapper functions
					setter.invoke(objectUnderConstruction, relationshipList);
					return objectUnderConstruction;
				});

			} else {
				throw new MappingException(String.format("Value of the relationship %s is invalid.", entry));
			}
		} else {
			return relationshipListToTargetClass(entry, targetClass, relationShipMap)
					.map(relatedEntities -> {
						// we return the constructed object, since invoke most likely returns null, which is not allowed on mapper functions
						setter.invoke(objectUnderConstruction, relatedEntities);
						return objectUnderConstruction;
					});
		}
	}

	/**
	 * Handle the evaluation of a relationship entry. Returns a single, emitting the target object, while invoking the property setting method.
	 *
	 * @param entry                   the entry containing the relationship-list
	 * @param objectUnderConstruction the object under construction
	 * @param relationShipMap         a map containing the pre-evaluated relationships
	 * @param setter                  the setter to be used for the property
	 * @param setterAnnotation        attribute setter annotation on the method
	 * @param <T>                     class of the objectUnderConstruction
	 * @return the single, emitting the objectUnderConstruction
	 */
	private <T> Single<T> handleRelationship(Map.Entry<String, Object> entry, T objectUnderConstruction, Map<String, EntityVO> relationShipMap, Method setter, AttributeSetter setterAnnotation) {
		Class<?> targetClass = setterAnnotation.targetClass();
		if (setterAnnotation.fromProperties()) {
			if (entry.getValue() instanceof Map relationshipRepresentation) {
				return relationshipFromProperties(relationshipRepresentation, targetClass)
						.map(relatedEntity -> {
							// we return the constructed object, since invoke most likely returns null, which is not allowed on mapper functions
							setter.invoke(objectUnderConstruction, relatedEntity);
							return objectUnderConstruction;
						});
			}
			throw new MappingException(String.format("Did not receive a valid relationship: %s", entry));
		} else {
			return getObjectFromRelationship(entry, targetClass, relationShipMap)
					.map(relatedEntity -> {
						// we return the constructed object, since invoke most likely returns null, which is not allowed on mapper functions
						setter.invoke(objectUnderConstruction, relatedEntity);
						return objectUnderConstruction;
					});
		}
	}

	/**
	 * Create the target object of a relationship from its properties(instead of entities additionally retrieved)
	 *
	 * @param relationShipRepresentation representation of the current relationship(as provided by the original entitiy)
	 * @param targetClass                class of the target object to be created(e.g. the object representing the relationship)
	 * @param <T>                        the class
	 * @return a single emitting the object representing the relationship
	 */
	private <T> Single<T> relationshipFromProperties(Map<String, Object> relationShipRepresentation, Class<T> targetClass) {
		try {
			String entityID;
			if (relationShipRepresentation.get(REALTIONSHIP_OBJECT_KEY) instanceof String id) {
				entityID = id;
			} else {
				throw new MappingException(String.format("Received an invalid relationship: %s", relationShipRepresentation));
			}
			Constructor<T> objectConstructor = targetClass.getDeclaredConstructor(String.class);
			T constructedObject = objectConstructor.newInstance(entityID);

			return Single.zip(relationShipRepresentation.entrySet().stream()
					.map(entry ->
					{
						Optional<Method> optionalSetter = getCorrespondingSetterMethod(constructedObject, entry.getKey());
						if (optionalSetter.isEmpty()) {
							// no setter for the field, can be ignored
							log.debug("No setter defined for field {}", entry.getKey());
							return Single.just(constructedObject);
						}
						Method setter = optionalSetter.get();
						Optional<AttributeSetter> optionalAttributeSetterAnnotation = getAttributeSetterAnnotation(setter);
						if (optionalAttributeSetterAnnotation.isEmpty()) {
							// no setter for the field, can be ignored
							log.debug("No setter defined for field {}", entry.getKey());
							return Single.just(constructedObject);
						}

						AttributeSetter attributeSetterAnnotation = optionalAttributeSetterAnnotation.get();

						Class<?> parameterType = getParameterType(setter.getParameterTypes());
						return switch (attributeSetterAnnotation.value()) {
							case RELATIONSHIP_LIST -> getRelationshipMap(relationShipRepresentation, targetClass)
									.flatMap(relationShips -> handleRelationshipList(
											entry,
											constructedObject,
											relationShips,
											setter,
											attributeSetterAnnotation));
							case RELATIONSHIP -> getRelationshipMap(relationShipRepresentation, targetClass)
									.flatMap(relationShips -> handleRelationship(entry, constructedObject, relationShips, setter, attributeSetterAnnotation));
							case PROPERTY, GEO_PROPERTY -> handleProperty(entry, constructedObject, setter, parameterType);
							case PROPERTY_LIST -> handlePropertyList(entry, constructedObject, setter, attributeSetterAnnotation);
							default -> throw new MappingException(String.format("Received type %s is not supported.", attributeSetterAnnotation.value()));
						};
					}).toList(), constructedObjects -> constructedObject);

		} catch (NoSuchMethodException e) {
			throw new MappingException(String.format("The class %s does not declare the required String id constructor.", targetClass));
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new MappingException(String.format("Was not able to create instance of %s.", targetClass), e);
		}
	}

	/**
	 * Returns a list of all entityIDs that are defined as relationships from the given entity.
	 *
	 * @param propertiesMap map of the properties to evaluate
	 * @param targetClass   target class of the mapping
	 * @param <T>           the class
	 * @return a list of uris
	 */
	private <T> List<URI> getRelationshipObjects(Map<String, Object> propertiesMap, Class<T> targetClass) {
		return Arrays.stream(targetClass.getMethods())
				.map(this::getAttributeSetterAnnotation)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.filter(a -> (a.value().equals(AttributeType.RELATIONSHIP) || a.value().equals(AttributeType.RELATIONSHIP_LIST)))
				// we don't need to retrieve entities that should be filled from the properties.
				.filter(a -> !a.fromProperties())
				.flatMap(attributeSetter -> getEntityURIsByAttributeSetter(attributeSetter, propertiesMap).stream())
				.toList();

	}

	/**
	 * Evaluate a properties map to get all referenced entity ids
	 *
	 * @param attributeSetter the attribute setter annotation
	 * @param propertiesMap   the properties map to check
	 * @return a list of entity ids
	 */
	private List<URI> getEntityURIsByAttributeSetter(AttributeSetter attributeSetter, Map<String, Object> propertiesMap) {
		List<String> idList = Optional.ofNullable(propertiesMap.get(attributeSetter.targetName()))
				.map(this::getURIsFromRelationshipObject)
				.orElseGet(List::of);
		return idList.stream().map(URI::create).toList();
	}

	/**
	 * Evaluate a concrete object of a realitonship. If its a list of objects, get the ids of all entities.
	 *
	 * @param relationShipObject the object to evaluate
	 * @return a list of all referenced ids
	 */
	private List<String> getURIsFromRelationshipObject(Object relationShipObject) {
		if (relationShipObject instanceof Map relationshipMap) {
			List<String> uriList = new ArrayList<>();
			uriList.add((String) relationshipMap.get(REALTIONSHIP_OBJECT_KEY));
			return uriList;
		} else if (relationShipObject instanceof List relationshipList) {
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
	private <T> Single<List<T>> relationshipListToTargetClass(Map.Entry<String, Object> entry, Class<T> targetClass, Map<String, EntityVO> relationShipEntitiesMap) {
		if (entry.getValue() instanceof Map) {
			return getObjectFromRelationship(entry, targetClass, relationShipEntitiesMap).map(List::of);
		} else if (entry.getValue() instanceof List relationshipMap) {
			return zipToList(relationshipMap.stream(), targetClass, relationShipEntitiesMap);

		}
		throw new MappingException(String.format("Did not receive a valid entry: %s", entry));
	}

	/**
	 * Helper method for combining the evaluation of relationship entities to a single result lits
	 *
	 * @param entries                 the relationship entries to evaluate
	 * @param targetClass             target class of the relationship object
	 * @param relationShipEntitiesMap map of the preevaluated relationship entities
	 * @param <T>                     target class of the relationship
	 * @return a single emitting the full list
	 */
	private <T> Single<List<T>> zipToList(Stream<Map<String, Object>> entries, Class<T> targetClass, Map<String, EntityVO> relationShipEntitiesMap) {
		return Single.zip(
				entries.map(map -> map.get(REALTIONSHIP_OBJECT_KEY))
						.filter(Objects::nonNull)
						.map(relationShipEntitiesMap::get)
						.map(entity -> fromEntityVO(entity, targetClass))
						.toList(),
				oList -> Arrays.stream(oList).map(targetClass::cast).toList()
		);
	}

	/**
	 * Method to translate a Map-Entry(e.g. NGSI-LD property) to a typed list as defined by the target object
	 *
	 * @param entry       attribute of the entity, e.g. a property or a list of properties
	 * @param targetClass class to be used as type for the typed list
	 * @param <T>         the type
	 * @return a list of objects, mapping the relationship
	 */
	private <T> List<T> propertyListToTargetClass(Map.Entry<String, Object> entry, Class<T> targetClass) {
		if (entry.getValue() instanceof Map entryMap) {
			if (entryMap.get(PROPERTY_VALUE_KEY) instanceof Map propertyMap) {
				return List.of(objectMapper.convertValue(propertyMap, targetClass));
			} else if (entryMap.get(PROPERTY_VALUE_KEY) instanceof List propertyList) {
				return propertyList.stream().map(propertyEntry -> objectMapper.convertValue(propertyEntry, targetClass)).toList();
			}
		}
		throw new MappingException(String.format("Did not receive a valid entry: %s", entry));
	}

	/**
	 * Retrieve the object from a relationship and return it as a java object of class T. All sub relationships will be evaluated, too.
	 *
	 * @param entry       the relationship entry
	 * @param targetClass the target-class of the entry
	 * @param <T>         the class
	 * @return the actual object
	 */
	private <T> Single<T> getObjectFromRelationship(Map.Entry<String, Object> entry, Class<T> targetClass, Map<String, EntityVO> relationShipEntitiesMap) {
		if (entry.getValue() instanceof Map relationshipMap) {
			Object relationshipObject = relationshipMap.get(REALTIONSHIP_OBJECT_KEY);
			if (!(relationshipObject instanceof String)) {
				throw new MappingException(String.format("Target of the relationship %s cannot be cast to string.", entry));
			}
			return fromEntityVO(relationShipEntitiesMap.get(relationshipObject), targetClass);
		} else {
			throw new MappingException(String.format("The entry %s is not a valid relationship.", entry));
		}
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

	/**
	 * Get the attribute setter annotation from the given method, if it exists.
	 */
	private Optional<AttributeSetter> getAttributeSetterAnnotation(Method m) {
		return Arrays.stream(m.getAnnotations()).filter(AttributeSetter.class::isInstance)
				.findFirst()
				.map(AttributeSetter.class::cast);
	}

}
