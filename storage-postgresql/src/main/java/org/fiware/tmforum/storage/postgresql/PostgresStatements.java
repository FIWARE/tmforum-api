package org.fiware.tmforum.storage.postgresql;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Runs statements on a connection of their own, which is closed on completion, error and cancellation.
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

    /**
     * Runs {@code work} in one transaction: committed when it completes, rolled back on error or cancellation.
     */
    static <R> Mono<R> inTransaction(ConnectionFactory connectionFactory, Function<Connection, Mono<R>> work) {
        return Mono.usingWhen(connectionFactory.create(),
                connection -> Mono.from(connection.beginTransaction())
                        .then(Mono.defer(() -> work.apply(connection)))
                        .flatMap(result -> Mono.from(connection.commitTransaction()).thenReturn(result))
                        .switchIfEmpty(Mono.defer(() -> Mono.from(connection.commitTransaction()).then(Mono.empty()))),
                Connection::close,
                (connection, failure) -> rollbackAndClose(connection),
                PostgresStatements::rollbackAndClose);
    }

    /**
     * Runs one statement on a connection the caller owns, e.g. inside {@link #inTransaction}.
     */
    static <R> Flux<R> execute(Connection connection, String sql, Function<Statement, Statement> binder,
                               Function<Result, Publisher<R>> resultMapper) {
        return Flux.from(binder.apply(connection.createStatement(sql)).execute()).flatMap(resultMapper);
    }

    // the original failure is what the caller reports, a failing rollback must not replace it
    private static Mono<Void> rollbackAndClose(Connection connection) {
        return Mono.from(connection.rollbackTransaction())
                .onErrorResume(rollbackFailure -> Mono.empty())
                .then(Mono.from(connection.close()));
    }
}
