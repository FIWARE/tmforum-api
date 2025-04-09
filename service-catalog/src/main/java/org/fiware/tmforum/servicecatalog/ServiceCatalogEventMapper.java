package org.fiware.tmforum.servicecatalog;

import lombok.RequiredArgsConstructor;
import org.fiware.servicecatalog.model.ServiceCandidateVO;
import org.fiware.servicecatalog.model.ServiceCatalogVO;
import org.fiware.servicecatalog.model.ServiceCategoryVO;
import org.fiware.servicecatalog.model.ServiceSpecificationVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.service.ServiceCandidate;
import org.fiware.tmforum.service.ServiceCategory;
import org.fiware.tmforum.servicecatalog.domain.ServiceCatalog;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class ServiceCatalogEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(ServiceCandidate.TYPE_SERVICE_CANDIDATE, new EventMapping(ServiceCandidateVO.class, ServiceCandidate.class)),
				entry(ServiceCatalog.TYPE_SERVICE_CATALOG, new EventMapping(ServiceCatalogVO.class, ServiceCatalog.class)),
				entry(ServiceCategory.TYPE_SERVICE_CATEGORY, new EventMapping(ServiceCategoryVO.class, ServiceCategory.class)),
				entry(ServiceSpecification.TYPE_SERVICE_SPECIFICATION, new EventMapping(ServiceSpecificationVO.class, ServiceSpecification.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> targetClass) {
		if (targetClass == ServiceCandidate.class) {
			return tmForumMapper.map((ServiceCandidate) rawPayload);
		}
		if (targetClass == ServiceCatalog.class) {
			return tmForumMapper.map((ServiceCatalog) rawPayload);
		}
		if (targetClass == ServiceCategory.class) {
			return tmForumMapper.map((ServiceCategory) rawPayload);
		}
		if (targetClass == ServiceSpecification.class) {
			return tmForumMapper.map((ServiceSpecification) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
