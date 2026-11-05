package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.JsonLdContext;
import io.github.wistefan.mapping.JsonLdContextResolver;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonLdContextPreloaderTest {

	private static final long RESOLVE_TIMEOUT_SECONDS = 5;

	@Test
	void resolvesTheContextOffTheStartingThread() throws Exception {
		JsonLdContextResolver resolver = mock(JsonLdContextResolver.class);
		CompletableFuture<Thread> resolvingThread = new CompletableFuture<>();
		when(resolver.resolve()).thenAnswer(invocation -> {
			resolvingThread.complete(Thread.currentThread());
			return JsonLdContext.EMPTY;
		});

		new JsonLdContextPreloader(resolver).onServerStartup(null);

		assertNotEquals(Thread.currentThread(), resolvingThread.get(RESOLVE_TIMEOUT_SECONDS, TimeUnit.SECONDS),
				"The context should be retrieved at server startup, on a thread that may block.");
	}
}
