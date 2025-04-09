package org.fiware.tmforum.resourcecatalog;

import lombok.RequiredArgsConstructor;
import org.fiware.resourcecatalog.model.ResourceCandidateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogVO;
import org.fiware.resourcecatalog.model.ResourceCategoryVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.resource.ResourceCandidate;
import org.fiware.tmforum.resource.ResourceCategory;
import org.fiware.tmforum.resource.ResourceSpecification;
import org.fiware.tmforum.resourcecatalog.domain.ResourceCatalog;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class ResourceCatalogEventMapper implements EventMapper {

	private final TMForumMapper tmForumMapper;

	@Override
	public Map<String, EventMapping> getEntityClassMapping() {
		return Map.ofEntries(
				entry(ResourceCandidate.TYPE_RESOURCE_CANDIDATE, new EventMapping(ResourceCandidateVO.class, ResourceCandidate.class)),
				entry(ResourceCatalog.TYPE_RESOURCE_CATALOG, new EventMapping(ResourceCatalogVO.class, ResourceCatalog.class)),
				entry(ResourceCategory.TYPE_RESOURCE_CATEGORY, new EventMapping(ResourceCategoryVO.class, ResourceCategory.class)),
				entry(ResourceSpecification.TYPE_RESOURCE_SPECIFICATION, new EventMapping(ResourceSpecificationVO.class, ResourceSpecification.class))
		);
	}

	@Override
	public Object mapPayload(Object rawPayload, Class<?> rawClass) {
		if (rawClass == ResourceCandidate.class) {
			return tmForumMapper.map((ResourceCandidate) rawPayload);
		}
		if (rawClass == ResourceCatalog.class) {
			return tmForumMapper.map((ResourceCatalog) rawPayload);
		}
		if (rawClass == ResourceCategory.class) {
			return tmForumMapper.map((ResourceCategory) rawPayload);
		}
		if (rawClass == ResourceSpecification.class) {
			return tmForumMapper.map((ResourceSpecification) rawPayload);
		}
		throw new TmForumException(String.format("Event-Payload %s is not supported.", rawPayload), TmForumExceptionReason.INVALID_DATA);
	}
}
