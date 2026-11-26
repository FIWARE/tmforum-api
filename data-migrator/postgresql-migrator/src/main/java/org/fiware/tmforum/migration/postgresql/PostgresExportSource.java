package org.fiware.tmforum.migration.postgresql;

import io.micronaut.context.ApplicationContext;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.postgresql.PostgresEntityRepository;

import java.util.List;

/**
 * Reads the target context's PostgreSQL through its {@link PostgresEntityRepository}, whose unfiltered list is in
 * creation order, so paging neither skips nor repeats while nothing is written.
 */
class PostgresExportSource implements ExportSource {

	private final PostgresEntityRepository repository;

	PostgresExportSource(ApplicationContext target) {
		this.repository = target.getBean(PostgresEntityRepository.class);
	}

	@Override
	public Page page(String type, Class<?> domainClass, int offset, int limit) {
		PagedResult<?> result = repository.findEntities(offset, limit, domainClass, null, null, type).block();
		if (result == null) {
			return new Page(List.of(), 0);
		}
		return new Page(List.copyOf(result.items()), result.totalCount() == null ? 0 : result.totalCount());
	}
}
