package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.JsonLdContextResolver;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Retrieves the {@code mapping.context-url} document once the server is up, on a blocking-capable thread, so the first
 * read that compacts against it does not fetch it on the event loop. Not earlier: a failed retrieval suspends
 * compaction for {@code mapping.context-retrieval-retry-delay}, and a context served next to the app may not be up yet.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class JsonLdContextPreloader {

	private final JsonLdContextResolver contextResolver;

	@EventListener
	public void onServerStartup(ServerStartupEvent event) {
		Mono.fromCallable(contextResolver::resolve)
				.subscribeOn(Schedulers.boundedElastic())
				.subscribe(context -> {
				}, e -> log.warn("Was not able to preload the JSON-LD context.", e));
	}
}
