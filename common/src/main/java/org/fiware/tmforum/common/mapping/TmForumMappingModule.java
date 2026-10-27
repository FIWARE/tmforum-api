package org.fiware.tmforum.common.mapping;

import com.networknt.schema.SchemaRegistry;
import jakarta.inject.Singleton;
import org.fiware.ngsi.model.GeoPropertyListVO;
import org.fiware.ngsi.model.PropertyListVO;
import org.fiware.ngsi.model.RelationshipListVO;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;

/**
 * Jackson customisations of the application mapper: the {@link ListVOMixin}s and the {@link ValidatingDeserializer}
 * for {@code @schemaLocation} validation. Added by Micronaut's ObjectMapperFactory, so hand-built mappers (e.g. the
 * mapping library's cache mapper) do not get it.
 */
@Singleton
public class TmForumMappingModule extends SimpleModule {

	public static final String MODULE_NAME = "TmForumMappingModule";

	public TmForumMappingModule(List<SubTypePropertyProvider> subTypePropertyProviders, SchemaRegistry schemaRegistry) {
		super(MODULE_NAME);
		setMixInAnnotation(PropertyListVO.class, ListVOMixin.class);
		setMixInAnnotation(RelationshipListVO.class, ListVOMixin.class);
		setMixInAnnotation(GeoPropertyListVO.class, ListVOMixin.class);
		setDeserializerModifier(new ValueDeserializerModifier() {
			@Override
			public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config,
														   BeanDescription.Supplier beanDescRef,
														   ValueDeserializer<?> deserializer) {
				return new ValidatingDeserializer(deserializer, beanDescRef.get(), subTypePropertyProviders,
						schemaRegistry);
			}
		});
	}
}
