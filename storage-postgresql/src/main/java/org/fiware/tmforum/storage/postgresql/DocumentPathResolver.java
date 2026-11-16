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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

    private final ObjectMapper mapper;
    private final ConcurrentMap<Resolution, List<String>> resolved = new ConcurrentHashMap<>();

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
        return resolved.computeIfAbsent(new Resolution(queryClass, path.segments()), this::walk);
    }

    private List<String> walk(Resolution resolution) {
        List<String> keys = new ArrayList<>();
        JavaType current = mapper.constructType(resolution.queryClass());
        for (String segment : resolution.segments()) {
            Optional<BeanPropertyDefinition> property = Optional.ofNullable(current).flatMap(type -> find(type, segment));
            keys.add(property.map(BeanPropertyDefinition::getName).orElse(segment));
            current = property.map(found -> elementType(found.getPrimaryType())).orElse(null);
        }
        return List.copyOf(keys);
    }

    private Optional<BeanPropertyDefinition> find(JavaType type, String segment) {
        if (ClassUtil.isJDKClass(type.getRawClass()) || type.isPrimitive()) {
            return Optional.empty();
        }
        List<BeanPropertyDefinition> properties = serializedProperties(type);
        return named(properties, segment)
                .or(() -> bridgeTarget(type.getRawClass(), segment).flatMap(target -> named(properties, target)));
    }

    private List<BeanPropertyDefinition> serializedProperties(JavaType type) {
        SerializationConfig config = mapper.serializationConfig();
        ClassIntrospector introspector = config.classIntrospectorInstance();
        BeanDescription description =
                introspector.introspectForSerialization(type, introspector.introspectClassAnnotations(type));
        return description.findProperties().stream().filter(BeanPropertyDefinition::couldSerialize).toList();
    }

    // a segment is a bean name; a key only matches if no bean name does
    private static Optional<BeanPropertyDefinition> named(List<BeanPropertyDefinition> properties, String name) {
        return properties.stream().filter(property -> property.getInternalName().equals(name)).findFirst()
                .or(() -> properties.stream().filter(property -> property.getName().equals(name)).findFirst());
    }

    // a getter the document does not serialize, which tells the mapping library which property holds its value
    private static Optional<String> bridgeTarget(Class<?> type, String segment) {
        String suffix = segment.substring(0, 1).toUpperCase(Locale.ROOT) + segment.substring(1);
        return Arrays.stream(type.getMethods())
                .filter(method -> method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers()))
                .filter(method -> GETTER_PREFIXES.stream().anyMatch(prefix -> method.getName().equals(prefix + suffix)))
                .map(method -> method.getAnnotation(AttributeGetter.class))
                .filter(getter -> getter != null && !getter.targetName().isEmpty())
                .map(AttributeGetter::targetName)
                .findFirst();
    }

    // lists and arrays are walked through to their elements, like the lax jsonpath does
    private static JavaType elementType(JavaType type) {
        return type.isCollectionLikeType() || type.isArrayType() ? type.getContentType() : type;
    }

    private record Resolution(Class<?> queryClass, List<String> segments) {
    }
}
