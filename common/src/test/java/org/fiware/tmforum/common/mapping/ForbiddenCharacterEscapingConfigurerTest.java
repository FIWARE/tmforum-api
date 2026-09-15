package org.fiware.tmforum.common.mapping;

import io.micronaut.context.event.BeanCreatedEvent;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ForbiddenCharacterEscapingConfigurerTest {

	private final ForbiddenCharacterEscapingConfigurer configurer = new ForbiddenCharacterEscapingConfigurer();

	@AfterEach
	public void resetToDefault() {
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	@Test
	public void propagatesDisabledFlagToTheEscaper() {
		GeneralProperties generalProperties = new GeneralProperties();
		generalProperties.setEscapeForbiddenCharacters(false);
		BeanCreatedEvent<GeneralProperties> event = mockEvent(generalProperties);

		configurer.onCreated(event);

		assertEquals("a=b", ForbiddenCharacterEscaper.escape("a=b"), "disabled means values pass through unchanged");
	}

	@Test
	public void propagatesEnabledFlagToTheEscaper() {
		GeneralProperties generalProperties = new GeneralProperties();
		generalProperties.setEscapeForbiddenCharacters(true);
		BeanCreatedEvent<GeneralProperties> event = mockEvent(generalProperties);
		ForbiddenCharacterEscaper.setEnabled(false);

		configurer.onCreated(event);

		assertEquals("a%3Db", ForbiddenCharacterEscaper.escape("a=b"));
	}

	@Test
	public void returnsTheSameBeanUnchanged() {
		GeneralProperties generalProperties = new GeneralProperties();
		BeanCreatedEvent<GeneralProperties> event = mockEvent(generalProperties);

		GeneralProperties returned = configurer.onCreated(event);

		assertEquals(generalProperties, returned);
	}

	private BeanCreatedEvent<GeneralProperties> mockEvent(GeneralProperties generalProperties) {
		@SuppressWarnings("unchecked")
		BeanCreatedEvent<GeneralProperties> event = mock(BeanCreatedEvent.class);
		when(event.getBean()).thenReturn(generalProperties);
		return event;
	}
}
