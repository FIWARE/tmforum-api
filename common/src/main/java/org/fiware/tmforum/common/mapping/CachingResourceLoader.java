package org.fiware.tmforum.common.mapping;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.ResourceLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

/**
 * {@link ResourceLoader} decorator, keeping the resources retrieved by its delegate in an in-memory cache with
 * a time-to-live. Schemas referenced through {@code @schemaLocation} are retrieved on every deserialization of
 * an entity otherwise, which adds a blocking download to each individual request.
 *
 * <p>The cache is bounded in both time and size: entries expire after the configured time-to-live, so that
 * changes at the schema provider are picked up eventually, and the least recently used entries are evicted
 * once the configured maximum number of schemas is exceeded. Since the schema addresses are provided by the
 * clients, an unbounded cache would allow them to fill up the memory.</p>
 *
 * <p>Failed retrievals are not cached, e.g. a temporarily unavailable schema provider is retried with the next
 * request.</p>
 */
@Slf4j
public class CachingResourceLoader implements ResourceLoader {

	private final ResourceLoader delegate;
	private final Cache<String, byte[]> resourceCache;

	/**
	 * Create a new caching loader, using the system ticker for expiring its entries.
	 *
	 * @param delegate    the loader to be used for actually retrieving the resources
	 * @param timeToLive  time a retrieved resource is kept, before it is retrieved again
	 * @param maximumSize maximum number of resources to be kept
	 */
	public CachingResourceLoader(ResourceLoader delegate, Duration timeToLive, long maximumSize) {
		this(delegate, timeToLive, maximumSize, Ticker.systemTicker());
	}

	/**
	 * Create a new caching loader with an explicit ticker, to allow deterministic testing of the expiry.
	 *
	 * @param delegate    the loader to be used for actually retrieving the resources
	 * @param timeToLive  time a retrieved resource is kept, before it is retrieved again
	 * @param maximumSize maximum number of resources to be kept
	 * @param ticker      the time source to be used for the expiry
	 */
	CachingResourceLoader(ResourceLoader delegate, Duration timeToLive, long maximumSize, Ticker ticker) {
		this.delegate = delegate;
		this.resourceCache = CacheBuilder.newBuilder()
				.expireAfterWrite(timeToLive)
				.maximumSize(maximumSize)
				.ticker(ticker)
				.build();
	}

	@Override
	public InputStreamSource getResource(AbsoluteIri location) {
		InputStreamSource delegateSource = delegate.getResource(location);
		if (delegateSource == null || location == null) {
			// the delegate is not responsible for the given address, the next loader of the chain has to take over
			return delegateSource;
		}
		String resourceAddress = location.toString();
		// the resource is only retrieved when the stream is actually requested, therefore the caching has to
		// happen at that point, too.
		return () -> new ByteArrayInputStream(getResourceData(resourceAddress, delegateSource));
	}

	/**
	 * Get the data for the given address, either from the cache or - if it is not cached (anymore) - from the
	 * delegate.
	 */
	private byte[] getResourceData(String resourceAddress, InputStreamSource delegateSource) throws IOException {
		try {
			return resourceCache.get(resourceAddress, () -> retrieveResourceData(resourceAddress, delegateSource));
		} catch (ExecutionException | UncheckedExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException ioException) {
				throw ioException;
			}
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IOException("Was not able to retrieve resource %s.".formatted(resourceAddress), cause);
		}
	}

	/**
	 * Retrieve the resource from the delegate and read it into memory, so that it can be handed out repeatedly.
	 */
	private byte[] retrieveResourceData(String resourceAddress, InputStreamSource delegateSource) throws IOException {
		log.debug("Resource {} is not cached, retrieve it.", resourceAddress);
		try (InputStream inputStream = delegateSource.getInputStream()) {
			return inputStream.readAllBytes();
		}
	}
}
