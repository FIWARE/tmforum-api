package org.fiware.tmforum.storage.postgresql;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.storage.NotStored;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedConstructor;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Lets the document mapper instantiate domain entities, which only have an id constructor, and optionally drops
 * {@link NotStored} fields. Registered only on the document mappers, never on the application mapper.
 */
class DocumentAnnotationIntrospector extends JacksonAnnotationIntrospector {

    static final String ID_PROPERTY = "id";

    // preferred parameter types of the id constructor, some reference classes have one per type
    private static final List<Class<?>> ID_TYPES = List.of(String.class, URI.class);

    private final boolean dropNotStored;

    DocumentAnnotationIntrospector(boolean dropNotStored) {
        this.dropNotStored = dropNotStored;
    }

    @Override
    public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated a) {
        JsonCreator.Mode declared = super.findCreatorAnnotation(config, a);
        if (declared == null && a instanceof AnnotatedConstructor constructor && isIdConstructor(constructor)) {
            return JsonCreator.Mode.PROPERTIES;
        }
        return declared;
    }

    @Override
    public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember m) {
        if (m instanceof AnnotatedParameter parameter
                && parameter.getOwner() instanceof AnnotatedConstructor constructor
                && isIdConstructor(constructor)) {
            return ID_PROPERTY;
        }
        return super.findImplicitPropertyName(config, m);
    }

    @Override
    public boolean hasIgnoreMarker(MapperConfig<?> config, AnnotatedMember m) {
        return (dropNotStored && m.hasAnnotation(NotStored.class)) || super.hasIgnoreMarker(config, m);
    }

    private static boolean isIdConstructor(AnnotatedConstructor constructor) {
        Constructor<?> candidate = constructor.getAnnotated();
        return idConstructor(candidate.getDeclaringClass()).filter(candidate::equals).isPresent();
    }

    /**
     * The single-argument constructor of an {@link Entity} subclass without a no-arg one, preferring a String id.
     */
    static Optional<Constructor<?>> idConstructor(Class<?> type) {
        if (!Entity.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers())) {
            return Optional.empty();
        }
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (Arrays.stream(constructors).anyMatch(c -> c.getParameterCount() == 0)) {
            return Optional.empty();
        }
        return ID_TYPES.stream()
                .flatMap(idType -> Arrays.stream(constructors)
                        .filter(c -> c.getParameterCount() == 1 && c.getParameterTypes()[0] == idType))
                .findFirst();
    }
}
