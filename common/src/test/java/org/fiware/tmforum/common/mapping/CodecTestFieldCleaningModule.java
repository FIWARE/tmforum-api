package org.fiware.tmforum.common.mapping;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Registers the {@link FieldCleaningSerializer} for {@link CodecTestController.FieldCleanedVO}.
 */
@Requires(property = StubBrokerController.SPEC_NAME_PROPERTY, value = MapperCodecRegressionTest.SPEC_NAME)
@Singleton
public class CodecTestFieldCleaningModule extends FieldCleaningModule {

	public CodecTestFieldCleaningModule() {
		super(List.of(CodecTestController.FieldCleanedVO.class));
	}
}
