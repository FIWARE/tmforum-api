package org.fiware.tmforum.agreement;

import lombok.RequiredArgsConstructor;
import org.fiware.agreement.model.AgreementSpecificationVO;
import org.fiware.agreement.model.AgreementVO;
import org.fiware.tmforum.agreement.domain.Agreement;
import org.fiware.tmforum.agreement.domain.AgreementSpecification;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class AgreementEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(Agreement.TYPE_AGREEMENT, new EventMapping(AgreementVO.class, Agreement.class)),
				entry(AgreementSpecification.TYPE_AGREEMENT_SPECIFICATION, new EventMapping(AgreementSpecificationVO.class, AgreementSpecification.class))
		);
	}


	@Override
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (rawClass == AgreementSpecification.class) {
			return tmForumMapper.map((AgreementSpecification) rawPayload);
		}
		if (rawClass == Agreement.class) {
			return tmForumMapper.map((Agreement) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
