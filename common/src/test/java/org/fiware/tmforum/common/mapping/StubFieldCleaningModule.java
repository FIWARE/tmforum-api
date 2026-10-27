package org.fiware.tmforum.common.mapping;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Test-only {@link FieldCleaningModule} bean, used to check that ObjectMapperFactory picks up such modules.
 */
@Requires(property = StubBrokerController.SPEC_NAME_PROPERTY, value = NgsiLdMappingRegressionTest.SPEC_NAME)
@Singleton
public class StubFieldCleaningModule extends FieldCleaningModule {

	public StubFieldCleaningModule() {
		super(List.of(StubVO.class));
	}

	/**
	 * Placeholder VO; registering a real one would change how other beans serialize it.
	 */
	public record StubVO(String id) {
	}
}
