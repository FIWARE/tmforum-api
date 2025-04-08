package org.fiware.tmforum.party;

import lombok.RequiredArgsConstructor;
import org.fiware.party.model.IndividualVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.party.domain.organization.Organization;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class PartyCatalogEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(Individual.TYPE_INDIVIDUAL, new EventMapping(IndividualVO.class, Individual.class)),
				entry(Organization.TYPE_ORGANIZATION, new EventMapping(OrganizationVO.class, Organization.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> targetClass) {
		if (targetClass == Individual.class) {
			return tmForumMapper.map((Individual) rawPayload);
		}
		if (targetClass == Organization.class) {
			return tmForumMapper.map((Organization) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
