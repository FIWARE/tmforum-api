package org.fiware.tmforum.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mapper to handle translation from NGSI-LD entities to Java-Objects, based on annotations added to the target class
 */
@Slf4j
@RequiredArgsConstructor
public class EntityVOMapper extends Mapper {

	public static final String REALTIONSHIP_OBJECT_KEY = "object";

	private final ObjectMapper objectMapper;

	/**
	 * Method to map an NGSI-LD Entity into a Java-Object of class targetClass. The class has to provide a string constructor to receive the entity id
	 *
	 * @param entityVO    the NGSI-LD entity to be mapped
	 * @param targetClass class of the target object
	 * @param <T>         generic type of the target object, has to extend provide a string-constructor to receive the entity id
	 * @return the mapped object
	 */
	public <T> T fromEntityVO(EntityVO entityVO, Class<T> targetClass) {

		MappingEnabled mappingEnabled = isMappingEnabled(targetClass).orElseThrow(() -> new MappingException(String.format("Mapping is not enabled for class %s", targetClass)));

		if (!entityVO.getType().equals(mappingEnabled.entityType())) {
			throw new MappingException(String.format("Entity and Class type do not match - %s vs %s.", entityVO.getType(), mappingEnabled.entityType()));
		}

		try {
			Constructor<T> objectConstructor = targetClass.getDeclaredConstructor(String.class);
			T constructedObject = objectConstructor.newInstance(entityVO.getId().toString());

			entityVO.getAdditionalProperties().entrySet().forEach(
					entry -> {
						Optional<Method> optionalSetter = getCorrespondingSetterMethod(constructedObject, entry.getKey());
						if (optionalSetter.isEmpty()) {
							log.warn("Ignoring property {} for entity {} since there is no mapping configured.", entry.getKey(), entityVO.getId());
							return;
						}
						Method setterMethod = optionalSetter.get();
						AttributeSetter setterAnnotation = getAttributeSetterAnnotation(setterMethod).get();
						Class<?> parameterType = getParameterType(setterMethod.getParameterTypes());

						try {
							switch (setterAnnotation.value()) {
								case PROPERTY, GEO_PROPERTY -> {
									if (entry.getValue() instanceof Map propertyMap) {
										optionalSetter.get().invoke(constructedObject, objectMapper.convertValue(propertyMap.get("value"), parameterType));
									} else {
										throw new MappingException(String.format("Value of the property %s was not a map.", entry));
									}
								}
								case RELATIONSHIP -> {
									Class<?> targetClassMapping = setterAnnotation.targetClass();
									optionalSetter.get().invoke(constructedObject, getObjectFromRelationship(entry, targetClassMapping));
								}
								case GEO_PROPERTY_LIST -> {
									// TODO: implement handling for lists of geo-properties
								}
								case RELATIONSHIP_LIST -> {
									Class<?> targetClassMapping = setterAnnotation.targetClass();
									optionalSetter.get().invoke(constructedObject, relationshipListToTargetClass(entry, targetClassMapping));
								}
								default -> throw new MappingException(String.format("Received type %s is not supported.", setterAnnotation.value()));
							}
						} catch (IllegalAccessException | InvocationTargetException e) {
							log.error("Was not able to set property {} for entity {}", entry.getKey(), entityVO.getId(), e);
						}
					}
			);
			return constructedObject;
		} catch (NoSuchMethodException e) {
			throw new MappingException(String.format("The class %s does not declare the required String id constructor.", targetClass));
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new MappingException(String.format("Was not able to create instance of %s.", targetClass), e);
		}
	}

	/**
	 * Method to translate a Map-Entry(e.g. NGSI-LD relationship) to a typed list as defined by the target object
	 *
	 * @param entry       attribute of the entity, e.g. a relationship or a list of relationships
	 * @param targetClass class to be used as type for the typed list
	 * @param <T>         the type
	 * @return a list of objects, mapping the relationship
	 */
	private <T> List<T> relationshipListToTargetClass(Map.Entry<String, Object> entry, Class<T> targetClass) {
		if (entry.getValue() instanceof Map) {
			return List.of(getObjectFromRelationship(entry, targetClass));
		} else if (entry.getValue() instanceof List relationshipMap) {
			return relationshipMap.stream()
					.filter(relationshipEntry -> relationshipEntry instanceof Map.Entry)
					.map(relationshipEntry -> getObjectFromRelationship((Map.Entry<String, Object>) relationshipEntry, targetClass))
					.toList();
		}
		throw new MappingException(String.format("Did not receive a valid entry: %s", entry));
	}

	/**
	 * Retrieve the object from a relationship and return it as a java object of class T
	 *
	 * @param entry       the relationship entry
	 * @param targetClass the target-class of the entry
	 * @param <T>         the class
	 * @return the actual object
	 */
	private <T> T getObjectFromRelationship(Map.Entry<String, Object> entry, Class<T> targetClass) {
		try {
			if (entry.getValue() instanceof Map relationshipMap) {
				Constructor<T> declaredConstructor = targetClass.getDeclaredConstructor(String.class);
				Object relationshipObject = relationshipMap.get(REALTIONSHIP_OBJECT_KEY);
				if (!(relationshipObject instanceof String)) {
					throw new MappingException(String.format("Target of the relationship %s cannot be cast to string.", entry));
				}
				T constructedObject = declaredConstructor.newInstance(relationshipObject);
				getAttributeSettersMethods(constructedObject).forEach(method -> {
					AttributeSetter attributeSetter = getAttributeSetterAnnotation(method).get();
					Object attributeObject = relationshipMap.get(attributeSetter.targetName());
					if (attributeObject == null) {
						return;
					}
					if (!(attributeObject instanceof Map)) {
						throw new MappingException("The additional property needs to be a map.");
					}
					try {
						method.invoke(constructedObject, objectMapper.convertValue(((Map) attributeObject).get("value"), getTargetClass(attributeSetter, method)));
					} catch (IllegalAccessException | InvocationTargetException e) {
						throw new MappingException(String.format("Was not able to set value with method %s for the constructed object for class %s", method, targetClass), e);
					}
				});
				return constructedObject;
			} else {
				throw new MappingException(String.format("The entry %s is not a valid relationship.", entry));
			}
		} catch (NoSuchMethodException e) {
			throw new MappingException(String.format("Class %s does not declare a String id constructor that is required for relationship mappings.", targetClass));
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new MappingException(String.format("Was not able to instantiate %s", targetClass), e);
		}
	}

	/**
	 * Get the target class from an attribute setter. Use the methods return type as a default.
	 */
	private Class<?> getTargetClass(AttributeSetter setterAnnotation, Method m) {
		if (setterAnnotation.targetClass() != Object.class) {
			return setterAnnotation.targetClass();
		}
		return m.getReturnType();
	}

	/**
	 * Return the type of the setters parameter.
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
