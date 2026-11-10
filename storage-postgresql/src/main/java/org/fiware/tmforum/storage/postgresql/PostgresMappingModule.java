package org.fiware.tmforum.storage.postgresql;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.UnmappedProperty;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
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
        setMixInAnnotation(TMForumSubscription.class, TMForumSubscriptionMixIn.class);
        setSerializerModifier(new ExtensionPropertiesModifier());
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.insertAnnotationIntrospector(new DocumentAnnotationIntrospector(dropNotStored));
    }

    // the constructor derives the type, so a stored one is skipped instead of reaching the any-setter. Member-level:
    // a class-level @JsonIgnoreProperties is not merged, a subclass declaring its own would replace it.
    abstract static class EntityWithIdMixIn {

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        abstract String getType();
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

    // broker-facing, escaped views of query/rawQuery; the document keeps the original values
    abstract static class TMForumSubscriptionMixIn {

        @JsonIgnore
        abstract String getEscapedQuery();

        @JsonIgnore
        abstract void setEscapedQuery(String escapedQuery);

        @JsonIgnore
        abstract String getEscapedRawQuery();

        @JsonIgnore
        abstract void setEscapedRawQuery(String escapedRawQuery);
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
            // ignored accessors as well, since the read side skips their keys; a PostgreSQL-only drop (ticket 0)
            BeanDescription description = beanDesc.get();
            Set<String> declaredNames = new HashSet<>(description.getIgnoredPropertyNames());
            properties.forEach(property -> declaredNames.add(property.getName()));
            // properties only the read side has (setter-only, creator parameters) would take such a key on read
            config.classIntrospectorInstance()
                    .introspectForDeserialization(description.getType(), description.getClassInfo())
                    .findProperties()
                    .forEach(property -> declaredNames.add(property.getName()));
            // the read side never hands this key to the any-setter, so writing it would lose it silently
            declaredNames.add(Entity.ADDITIONAL_PROPERTIES_KEY);
            properties.add(new ExtensionPropertiesWriter(additionalProperties, declaredNames));
            return properties;
        }
    }

    /**
     * Writes each extension entry as a property, skipping names of declared properties so no key is duplicated;
     * of two entries with the same name the last one wins, and a null value is omitted like a null declared one.
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
                if (extension.getValue() == null) {
                    continue;
                }
                generator.writeName(extension.getKey());
                context.writeValue(generator, extension.getValue());
            }
        }
    }
}
