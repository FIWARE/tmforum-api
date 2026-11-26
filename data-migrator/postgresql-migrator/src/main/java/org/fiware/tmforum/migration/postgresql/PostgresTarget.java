package org.fiware.tmforum.migration.postgresql;

import io.micronaut.context.ApplicationContext;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.fiware.tmforum.storage.postgresql.PostgresEntityRepository;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import reactor.core.publisher.Flux;

import java.time.Instant;

/**
 * Writes into {@code tmforum.entity} through the target context's {@link PostgresEntityRepository}.
 */
class PostgresTarget implements EntityTarget {

	static final String COUNT_TYPE = "SELECT count(*) FROM " + PostgresStorage.QUALIFIED_ENTITY_TABLE + " WHERE type = $1";

	private final PostgresEntityRepository repository;
	private final ConnectionFactory connectionFactory;

	PostgresTarget(ApplicationContext target) {
		this.repository = target.getBean(PostgresEntityRepository.class);
		this.connectionFactory = target.getBean(ConnectionFactory.class);
	}

	@Override
	public long count(String type) {
		Long count = Flux.usingWhen(connectionFactory.create(),
						connection -> Flux.from(connection.createStatement(COUNT_TYPE).bind(0, type).execute())
								.flatMap(result -> result.map(row -> row.get(0, Long.class))),
						Connection::close)
				.blockFirst();
		return count == null ? 0 : count;
	}

	@Override
	public void importEntity(Object entity, Instant createdAt, Instant updatedAt) {
		repository.importDomainEntity(entity, createdAt, updatedAt).block();
	}
}
