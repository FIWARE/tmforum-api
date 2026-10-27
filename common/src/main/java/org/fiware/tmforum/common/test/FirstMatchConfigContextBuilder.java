package org.fiware.tmforum.common.test;

import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.core.io.ResourceLoadStrategy;
import io.micronaut.core.io.ResourceLoadStrategyType;

/**
 * Test context builder that lets src/test/resources/application*.yaml shadow the main ones, as on Micronaut 4.
 * Micronaut 5 fails on duplicate configuration resources by default; use via {@code @MicronautTest(contextBuilder = ...)}.
 */
public class FirstMatchConfigContextBuilder extends DefaultApplicationContextBuilder {

	public FirstMatchConfigContextBuilder() {
		configurationLoadingStrategy(ResourceLoadStrategy.builder()
				.type(ResourceLoadStrategyType.FIRST_MATCH)
				.warnOnDuplicates(false));
	}
}
