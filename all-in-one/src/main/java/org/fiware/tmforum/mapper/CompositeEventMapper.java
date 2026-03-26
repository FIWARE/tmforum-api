package org.fiware.tmforum.mapper;

import io.micronaut.context.annotation.Primary;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.common.notification.ModuleEventMapper;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Singleton
@RequiredArgsConstructor
public class CompositeEventMapper implements EventMapper {

	private final Collection<ModuleEventMapper> moduleEventMappers;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return moduleEventMappers.stream()
				.flatMap(m -> m.getEntityClassMapping().entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> targetClass) {
		return moduleEventMappers.stream()
				.filter(m -> m.getEntityClassMapping().values().stream()
						.anyMatch(em -> em.rawClass().equals(targetClass)))
				.findFirst()
				.orElseThrow(() -> new TmForumException(
						String.format("No EventMapper found for class %s", targetClass.getName()),
						TmForumExceptionReason.UNKNOWN))
				.mapPayload(rawPayload, targetClass);
	}
}
