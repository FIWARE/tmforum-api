package org.fiware.tmforum.usagemanagement;

import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.usagemanagement.domain.Usage;
import org.fiware.tmforum.usagemanagement.domain.UsageSpecification;
import org.fiware.usagemanagement.model.UsageSpecificationVO;
import org.fiware.usagemanagement.model.UsageVO;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class UsageManagementEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(Usage.TYPE_U, new EventMapping(UsageVO.class, Usage.class)),
				entry(UsageSpecification.TYPE_USP, new EventMapping(UsageSpecificationVO.class, UsageSpecification.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> targetClass) {
		if (targetClass == Usage.class) {
			return tmForumMapper.map((Usage) rawPayload);
		}
		if (targetClass == UsageSpecification.class) {
			return tmForumMapper.map((UsageSpecification) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
