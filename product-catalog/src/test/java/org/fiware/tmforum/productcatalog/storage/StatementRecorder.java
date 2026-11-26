package org.fiware.tmforum.productcatalog.storage;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import jakarta.inject.Singleton;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wraps the R2DBC connection factory with r2dbc-proxy and records the SQL of every executed statement.
 * Only active with {@value #ENABLED_PROPERTY}, so the other ITs of the module keep the plain pool.
 */
@Singleton
@Requires(env = PostgresStorage.ENVIRONMENT)
@Requires(property = StatementRecorder.ENABLED_PROPERTY, value = "true")
public class StatementRecorder implements BeanCreatedEventListener<ConnectionFactory> {

	public static final String ENABLED_PROPERTY = "test.record-statements";

	private final List<String> statements = new CopyOnWriteArrayList<>();

	@Override
	public ConnectionFactory onCreated(BeanCreatedEvent<ConnectionFactory> event) {
		ConnectionFactory pool = event.getBean();
		ConnectionFactory proxy = ProxyConnectionFactory.builder(pool)
				.onBeforeQuery(execution -> statements.add(
						String.join("; ", execution.getQueries().stream().map(QueryInfo::getQuery).toList())))
				.build();
		return new ClosingProxy(proxy, pool);
	}

	/** Executed statements since the last {@link #reset()}, one entry per execution. */
	public List<String> statements() {
		return List.copyOf(statements);
	}

	public void reset() {
		statements.clear();
	}

	// the proxy is not Closeable, so ConnectionPoolCloser would leave the pool open without this
	private record ClosingProxy(ConnectionFactory proxy, ConnectionFactory pool) implements ConnectionFactory, Closeable {

		@Override
		public Publisher<? extends Connection> create() {
			return proxy.create();
		}

		@Override
		public ConnectionFactoryMetadata getMetadata() {
			return proxy.getMetadata();
		}

		@Override
		public Publisher<Void> close() {
			return ((Closeable) pool).close();
		}
	}
}
