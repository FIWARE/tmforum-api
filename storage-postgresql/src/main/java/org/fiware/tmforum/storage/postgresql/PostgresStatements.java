package org.fiware.tmforum.storage.postgresql;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * Runs one statement on its own connection, which is closed on completion, error and cancellation.
 */
final class PostgresStatements {

    private PostgresStatements() {
    }

    static <R> Flux<R> execute(ConnectionFactory connectionFactory, String sql, Function<Statement, Statement> binder,
                               Function<Result, Publisher<R>> resultMapper) {
        return Flux.usingWhen(connectionFactory.create(),
                connection -> Flux.from(binder.apply(connection.createStatement(sql)).execute())
                        .flatMap(resultMapper),
                Connection::close);
    }
}
