package org.fiware.tmforum.storage.postgresql;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.storage.query.AttributePath;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.util.ClassUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Maps the TMForum (bean) names of a declared query path to the keys the document mapper writes, which differ for
 * {@code @JsonProperty} renames and {@code @JsonIgnore}d bridge getters. Undeclared paths and segments that do not
 * resolve (e.g. subtype-only fields of a polymorphic query) are kept as they are.
 */
final class DocumentPathResolver {

    private static final List<String> GETTER_PREFIXES = List.of("get", "is");
    private static final TypeProperties NO_PROPERTIES = new TypeProperties(Map.of(), Map.of(), Map.of());

    private final ObjectMapper mapper;
    // keyed by type only, never by segment: segments can be client-chosen, the types are bounded by the domain model
    private final ConcurrentMap<JavaType, TypeProperties> introspected = new ConcurrentHashMap<>();

    DocumentPathResolver(DocumentMapper documentMapper) {
        this.mapper = documentMapper.objectMapper();
    }

    /**
     * The document keys of {@code path}, one per segment, with {@code queryClass} the class it was parsed against.
     */
    List<String> resolve(Class<?> queryClass, AttributePath path) {
        if (!path.declared()) {
            return path.segments();
        }
        List<String> keys = new ArrayList<>();
        JavaType current = mapper.constructType(queryClass);
        for (String segment : path.segments()) {
            Optional<Property> property = Optional.ofNullable(current).flatMap(type -> properties(type).find(segment));
            keys.add(property.map(Property::key).orElse(segment));
            current = property.map(Property::elementType).orElse(null);
        }
        return List.copyOf(keys);
    }

    /**
     * Number of types introspected so far, for tests.
     */
    int introspectedTypes() {
        return introspected.size();
    }

    private TypeProperties properties(JavaType type) {
        return introspected.computeIfAbsent(type, this::introspect);
    }

    private TypeProperties introspect(JavaType type) {
        if (ClassUtil.isJDKClass(type.getRawClass()) || type.isPrimitive()) {
            return NO_PROPERTIES;
        }
        Map<String, Property> byBeanName = new HashMap<>();
        Map<String, Property> byKey = new HashMap<>();
        for (BeanPropertyDefinition definition : serializedProperties(type)) {
            Property property = new Property(definition.getName(), elementType(definition.getPrimaryType()));
            byBeanName.putIfAbsent(definition.getInternalName(), property);
            byKey.putIfAbsent(definition.getName(), property);
        }
        return new TypeProperties(Map.copyOf(byBeanName), Map.copyOf(byKey), bridgeTargets(type.getRawClass()));
    }

    private List<BeanPropertyDefinition> serializedProperties(JavaType type) {
        SerializationConfig config = mapper.serializationConfig();
        ClassIntrospector introspector = config.classIntrospectorInstance();
        BeanDescription description =
                introspector.introspectForSerialization(type, introspector.introspectClassAnnotations(type));
        return description.findProperties().stream().filter(BeanPropertyDefinition::couldSerialize).toList();
    }

    // getters the document does not serialize, which tell the mapping library which property holds their value
    private static Map<String, String> bridgeTargets(Class<?> type) {
        Map<String, String> targets = new HashMap<>();
        for (Method method : type.getMethods()) {
            AttributeGetter getter = method.getAnnotation(AttributeGetter.class);
            if (getter != null && !getter.targetName().isEmpty() && method.getParameterCount() == 0
                    && !Modifier.isStatic(method.getModifiers())) {
                targets.putIfAbsent(method.getName(), getter.targetName());
            }
        }
        return Map.copyOf(targets);
    }

    // lists and arrays are walked through to their elements, like the lax jsonpath does
    private static JavaType elementType(JavaType type) {
        return type.isCollectionLikeType() || type.isArrayType() ? type.getContentType() : type;
    }

    private record Property(String key, JavaType elementType) {
    }

    private record TypeProperties(Map<String, Property> byBeanName, Map<String, Property> byKey,
                                  Map<String, String> bridgeTargetsByGetter) {

        // a segment is a bean name; a key only matches if no bean name does
        Optional<Property> find(String segment) {
            return named(segment).or(() -> bridgeTarget(segment).flatMap(this::named));
        }

        private Optional<Property> named(String name) {
            return Optional.ofNullable(byBeanName.get(name)).or(() -> Optional.ofNullable(byKey.get(name)));
        }

        private Optional<String> bridgeTarget(String segment) {
            String suffix = segment.substring(0, 1).toUpperCase(Locale.ROOT) + segment.substring(1);
            return GETTER_PREFIXES.stream()
                    .map(prefix -> bridgeTargetsByGetter.get(prefix + suffix))
                    .filter(Objects::nonNull)
                    .findFirst();
        }
    }
}
