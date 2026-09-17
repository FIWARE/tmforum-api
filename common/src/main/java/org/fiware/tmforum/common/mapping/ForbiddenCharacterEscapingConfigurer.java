package org.fiware.tmforum.common.mapping;

import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import org.fiware.tmforum.common.configuration.GeneralProperties;

/**
 * Bridges {@link GeneralProperties#isEscapeForbiddenCharacters()} into {@link ForbiddenCharacterEscaper}'s
 * static flag on startup. Needed because the domain objects that use the escaper (e.g.
 * {@code TMForumSubscription}) are constructed by the mapping library via reflection, not through
 * Micronaut DI, so they cannot have {@link GeneralProperties} injected directly.
 */
@Singleton
public class ForbiddenCharacterEscapingConfigurer implements BeanCreatedEventListener<GeneralProperties> {

	@Override
	public GeneralProperties onCreated(BeanCreatedEvent<GeneralProperties> event) {
		GeneralProperties generalProperties = event.getBean();
		ForbiddenCharacterEscaper.setEnabled(generalProperties.isEscapeForbiddenCharacters());
		return generalProperties;
	}
}
