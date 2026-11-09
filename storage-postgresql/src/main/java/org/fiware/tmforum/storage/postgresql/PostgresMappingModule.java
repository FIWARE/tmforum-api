package org.fiware.tmforum.storage.postgresql;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.UnmappedProperty;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stored-document format of domain entities: extension properties as top-level keys, {@code type} write-only, id
 * constructors as creators. A plain module for the document mappers only, so API, cache and event serialization
 * keep the application format.
 */
class PostgresMappingModule extends SimpleModule {

    static final String MODULE_NAME = "PostgresMappingModule";
    static final String TYPE_PROPERTY = "type";

    private final boolean dropNotStored;

    /**
     * {@code dropNotStored} leaves {@link org.fiware.tmforum.storage.NotStored} fields out in both directions.
     */
    PostgresMappingModule(boolean dropNotStored) {
        super(MODULE_NAME);
        this.dropNotStored = dropNotStored;
        setMixInAnnotation(EntityWithId.class, EntityWithIdMixIn.class);
        setMixInAnnotation(Entity.class, EntityMixIn.class);
        setMixInAnnotation(ReferencedEntity.class, ReferencedEntityMixIn.class);
        setSerializerModifier(new ExtensionPropertiesModifier());
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.insertAnnotationIntrospector(new DocumentAnnotationIntrospector(dropNotStored));
    }

    // the constructor derives the type, so a stored one is skipped instead of reaching the any-setter
    @JsonIgnoreProperties(value = TYPE_PROPERTY, allowGetters = true)
    abstract static class EntityWithIdMixIn {
    }

    // read: every key no declared property takes becomes an extension; write: see ExtensionPropertiesModifier
    abstract static class EntityMixIn {

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        abstract List<UnmappedProperty> getAdditionalProperties();

        @JsonAnySetter
        abstract void addAdditionalProperties(String propertyKey, Object value);
    }

    // validation metadata of the class, not data; not every reference class ignores it itself
    abstract static class ReferencedEntityMixIn {

        @JsonIgnore
        abstract List<String> getReferencedTypes();
    }

    /**
     * Replaces the {@code additionalProperties} list of every entity by its entries as top-level properties.
     */
    private static final class ExtensionPropertiesModifier extends ValueSerializerModifier {

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                         BeanDescription.Supplier beanDesc,
                                                         List<BeanPropertyWriter> beanProperties) {
            if (!Entity.class.isAssignableFrom(beanDesc.getBeanClass())) {
                return beanProperties;
            }
            List<BeanPropertyWriter> properties = new ArrayList<>(beanProperties);
            BeanPropertyWriter additionalProperties = properties.stream()
                    .filter(property -> Entity.ADDITIONAL_PROPERTIES_KEY.equals(property.getName()))
                    .findFirst()
                    .orElse(null);
            if (additionalProperties == null) {
                return beanProperties;
            }
            properties.remove(additionalProperties);
            Set<String> declaredNames = new HashSet<>(beanDesc.get().getIgnoredPropertyNames());
            properties.forEach(property -> declaredNames.add(property.getName()));
            properties.add(new ExtensionPropertiesWriter(additionalProperties, declaredNames));
            return properties;
        }
    }

    /**
     * Writes each extension entry as a property, skipping names of declared properties so no key is duplicated;
     * of two entries with the same name the last one wins.
     */
    private static final class ExtensionPropertiesWriter extends BeanPropertyWriter {

        private final Set<String> declaredNames;

        private ExtensionPropertiesWriter(BeanPropertyWriter additionalProperties, Set<String> declaredNames) {
            super(additionalProperties);
            this.declaredNames = Set.copyOf(declaredNames);
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator generator, SerializationContext context)
                throws Exception {
            if (!(get(bean) instanceof List<?> extensions)) {
                return;
            }
            Map<String, Object> extensionValues = new LinkedHashMap<>();
            for (Object extension : extensions) {
                if (extension instanceof UnmappedProperty property && property.getName() != null
                        && !declaredNames.contains(property.getName())) {
                    extensionValues.put(property.getName(), property.getValue());
                }
            }
            for (Map.Entry<String, Object> extension : extensionValues.entrySet()) {
                generator.writeName(extension.getKey());
                context.writeValue(generator, extension.getValue());
            }
        }
    }
}
