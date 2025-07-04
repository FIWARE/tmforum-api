package org.fiware.tmforum.migration.writer;

import org.fiware.ngsi.model.EntityFragmentVO;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.repository.TmForumRepository;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.util.List;

@Singleton
public class UpdateWriter {

	@Inject
	public TmForumRepository repository;

	public <T> Mono<Void> writeUpdate(String entityId, T theObject) {
		return repository.updateDomainEntity(entityId, theObject);
	}
}
