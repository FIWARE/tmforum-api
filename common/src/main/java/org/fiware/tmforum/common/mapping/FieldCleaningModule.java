package org.fiware.tmforum.common.mapping;

import tools.jackson.databind.module.SimpleModule;

import java.util.Collection;

/**
 * Registers the {@link FieldCleaningSerializer} for an API's VOs. Each API exposes a subclass as bean; its class name
 * is the registration id, so the modules of all APIs survive side by side in all-in-one.
 */
public abstract class FieldCleaningModule extends SimpleModule {

	protected FieldCleaningModule(Collection<Class<?>> voClasses) {
		super(FieldCleaningModule.class.getSimpleName());
		voClasses.forEach(this::addFieldCleaningSerializer);
	}

	@Override
	public Object getRegistrationId() {
		return getClass().getName();
	}

	// generic helper, since the serializer has to be registered per class
	private <T> void addFieldCleaningSerializer(Class<T> voClass) {
		addSerializer(voClass, new FieldCleaningSerializer<>());
	}
}
