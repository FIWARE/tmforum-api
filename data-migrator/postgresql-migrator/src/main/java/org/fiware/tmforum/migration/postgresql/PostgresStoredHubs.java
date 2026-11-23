package org.fiware.tmforum.migration.postgresql;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Drops the broker's subscription reference from the stored hubs in one transaction. Neither {@code updated_at} nor
 * the outbox is touched: this is a migration, not a client write.
 */
class PostgresStoredHubs implements StoredHubs {

	private static final String ENTITY = PostgresStorage.QUALIFIED_ENTITY_TABLE;
	// $1 the hub type, $2 the attribute; '->' instead of the '?' operator, which reads like a placeholder
	private static final String HOLDS_SUBSCRIPTION = "type = $1 AND data -> $2::text IS NOT NULL";
	static final String COUNT = String.format("SELECT count(*) FROM %s WHERE %s", ENTITY, HOLDS_SUBSCRIPTION);
	static final String DROP = String.format("UPDATE %s SET data = data - $2::text WHERE %s", ENTITY,
			HOLDS_SUBSCRIPTION);

	private final ConnectionFactory connectionFactory;

	PostgresStoredHubs(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	@Override
	public StoredHubs.Outcome dropSubscriptions(boolean dryRun) {
		StoredHubs.Outcome result = Mono.usingWhen(connectionFactory.create(),
				connection -> drop(connection, dryRun),
				Connection::close,
				(connection, failure) -> rollbackAndClose(connection),
				PostgresStoredHubs::rollbackAndClose).block();
		if (result == null) {
			throw new IllegalStateException("Dropping the hubs' subscription returned no result.");
		}
		return result;
	}

	private Mono<StoredHubs.Outcome> drop(Connection connection, boolean dryRun) {
		Mono<Long> counted = execute(connection, COUNT)
				.flatMap(result -> result.map(row -> row.get(0, Long.class)))
				.single();
		Mono<StoredHubs.Outcome> dropped = dryRun
				? counted.map(count -> new StoredHubs.Outcome(count, 0))
				: counted.flatMap(count -> execute(connection, DROP)
						.flatMap(Result::getRowsUpdated)
						.reduce(0L, Long::sum)
						.map(updated -> new StoredHubs.Outcome(count, updated)));
		return Mono.from(connection.beginTransaction())
				.then(dropped)
				.flatMap(result -> Mono.from(dryRun ? connection.rollbackTransaction() : connection.commitTransaction())
						.thenReturn(result));
	}

	private static Flux<Result> execute(Connection connection, String sql) {
		return Flux.defer(() -> {
			Statement statement = connection.createStatement(sql)
					.bind(0, TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION)
					.bind(1, RawAttributeCheck.SUBSCRIPTION_ATTRIBUTE);
			return Flux.from(statement.execute());
		});
	}

	// the original failure is what the report shows, a failing rollback must not replace it
	private static Mono<Void> rollbackAndClose(Connection connection) {
		return Mono.from(connection.rollbackTransaction())
				.onErrorResume(rollbackFailure -> Mono.empty())
				.then(Mono.from(connection.close()));
	}
}
