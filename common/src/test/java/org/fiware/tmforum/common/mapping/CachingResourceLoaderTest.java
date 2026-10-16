package org.fiware.tmforum.common.mapping;

import com.google.common.base.Ticker;
import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.ResourceLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the time- and size-bound caching of retrieved schema resources.
 */
class CachingResourceLoaderTest {

	private static final String SCHEMA_ADDRESS = "http://my-schema-provider.org/the-schema.json";
	private static final String OTHER_SCHEMA_ADDRESS = "http://my-schema-provider.org/the-other-schema.json";
	private static final String SCHEMA_CONTENT = "{\"type\":\"object\"}";
	private static final Duration TIME_TO_LIVE = Duration.ofMinutes(30);
	private static final long MAX_SIZE = 10L;

	/**
	 * Ticker that only advances when the test tells it to, to get deterministic expiry.
	 */
	private static class TestTicker extends Ticker {

		private long nanos = 0;

		@Override public long read() {
			return nanos;
		}

		void advance(Duration duration) {
			nanos += duration.toNanos();
		}
	}

	/**
	 * Delegate that counts how often a resource was actually retrieved.
	 */
	private static class CountingResourceLoader implements ResourceLoader {

		private final AtomicInteger retrievalCount = new AtomicInteger(0);
		private final IOException failure;

		CountingResourceLoader() {
			this(null);
		}

		CountingResourceLoader(IOException failure) {
			this.failure = failure;
		}

		@Override public InputStreamSource getResource(AbsoluteIri location) {
			return () -> {
				retrievalCount.incrementAndGet();
				if (failure != null) {
					throw failure;
				}
				return new ByteArrayInputStream(SCHEMA_CONTENT.getBytes(StandardCharsets.UTF_8));
			};
		}

		int getRetrievalCount() {
			return retrievalCount.get();
		}
	}

	private static String read(ResourceLoader loader, String address) throws IOException {
		try (InputStream inputStream = loader.getResource(AbsoluteIri.of(address)).getInputStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@DisplayName("Repeated retrievals within the time-to-live are served from the cache.")
	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 25 })
	void repeatedRetrievalsAreCached(int numberOfRetrievals) throws IOException {
		CountingResourceLoader delegate = new CountingResourceLoader();
		TestTicker ticker = new TestTicker();
		CachingResourceLoader cachingLoader = new CachingResourceLoader(delegate, TIME_TO_LIVE, MAX_SIZE, ticker);

		for (int i = 0; i < numberOfRetrievals; i++) {
			assertEquals(SCHEMA_CONTENT, read(cachingLoader, SCHEMA_ADDRESS),
					"The cached schema should be returned unchanged.");
		}

		assertEquals(1, delegate.getRetrievalCount(),
				"The schema should only have been retrieved once, independent of the number of usages.");
	}

	@DisplayName("Schemas are retrieved again, once the time-to-live is expired.")
	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5 })
	void expiredSchemasAreRetrievedAgain(int numberOfExpiries) throws IOException {
		CountingResourceLoader delegate = new CountingResourceLoader();
		TestTicker ticker = new TestTicker();
		CachingResourceLoader cachingLoader = new CachingResourceLoader(delegate, TIME_TO_LIVE, MAX_SIZE, ticker);

		read(cachingLoader, SCHEMA_ADDRESS);
		for (int i = 0; i < numberOfExpiries; i++) {
			ticker.advance(TIME_TO_LIVE.plusSeconds(1));
			read(cachingLoader, SCHEMA_ADDRESS);
		}

		assertEquals(numberOfExpiries + 1, delegate.getRetrievalCount(),
				"The schema should have been retrieved again after every expiry.");
	}

	@DisplayName("Schemas are still cached shortly before the time-to-live is reached.")
	@Test
	void schemasAreCachedUntilTheTimeToLiveIsReached() throws IOException {
		CountingResourceLoader delegate = new CountingResourceLoader();
		TestTicker ticker = new TestTicker();
		CachingResourceLoader cachingLoader = new CachingResourceLoader(delegate, TIME_TO_LIVE, MAX_SIZE, ticker);

		read(cachingLoader, SCHEMA_ADDRESS);
		ticker.advance(TIME_TO_LIVE.minusSeconds(1));
		read(cachingLoader, SCHEMA_ADDRESS);

		assertEquals(1, delegate.getRetrievalCount(),
				"The schema should not have been retrieved again before its expiry.");
	}

	@DisplayName("The least recently used schemas are evicted when the maximum size is exceeded.")
	@Test
	void schemasAreEvictedWhenTheCacheIsFull() throws IOException {
		CountingResourceLoader delegate = new CountingResourceLoader();
		CachingResourceLoader cachingLoader = new CachingResourceLoader(delegate, TIME_TO_LIVE, 1L, new TestTicker());

		read(cachingLoader, SCHEMA_ADDRESS);
		read(cachingLoader, OTHER_SCHEMA_ADDRESS);
		read(cachingLoader, SCHEMA_ADDRESS);

		assertEquals(3, delegate.getRetrievalCount(),
				"The first schema should have been evicted by the second one and retrieved again.");
	}

	@DisplayName("Failed retrievals are not cached and the failure is propagated.")
	@Test
	void failedRetrievalsAreNotCached() {
		IOException expectedFailure = new IOException("The schema provider is not available.");
		CountingResourceLoader delegate = new CountingResourceLoader(expectedFailure);
		CachingResourceLoader cachingLoader = new CachingResourceLoader(delegate, TIME_TO_LIVE, MAX_SIZE,
				new TestTicker());

		assertEquals(expectedFailure, assertThrows(IOException.class, () -> read(cachingLoader, SCHEMA_ADDRESS)),
				"The failure of the delegate should be propagated as-is.");
		assertEquals(expectedFailure, assertThrows(IOException.class, () -> read(cachingLoader, SCHEMA_ADDRESS)),
				"The failure of the delegate should be propagated as-is.");
		assertEquals(2, delegate.getRetrievalCount(),
				"A failed retrieval should not be cached, thus it should be retried.");
	}

	@DisplayName("If the delegate is not responsible for the address, the next loader gets its chance.")
	@Test
	void unhandledAddressesAreNotCached() {
		CachingResourceLoader cachingLoader = new CachingResourceLoader(location -> null, TIME_TO_LIVE, MAX_SIZE,
				new TestTicker());

		assertNull(cachingLoader.getResource(AbsoluteIri.of(SCHEMA_ADDRESS)),
				"Nothing should be returned, so that the next loader of the chain is used.");
	}
}
