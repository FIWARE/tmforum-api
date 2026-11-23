package org.fiware.tmforum.migration.postgresql;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * {@link ReplayClient} on the JDK client. Captured headers the client sets itself (e.g. {@code Host}) are dropped.
 */
class HttpReplayClient implements ReplayClient {

	// the JDK client rejects these, and a captured value would be wrong for the other deployment anyway
	static final Set<String> RESTRICTED_HEADERS = Set.of("host", "content-length", "connection", "expect", "upgrade",
			"transfer-encoding");
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String JSON = "application/json";

	private final HttpClient httpClient;
	private final Duration timeout;

	HttpReplayClient(Duration timeout) {
		this.timeout = timeout;
		this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
	}

	@Override
	public Response send(URI baseUrl, ReplayRequest request) {
		String base = baseUrl.toString().replaceAll("/+$", "");
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + request.path())).timeout(timeout);
		request.headers().forEach((name, value) -> {
			if (!RESTRICTED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
				builder.header(name, value);
			}
		});
		String body = request.bodyText();
		if (body != null && request.headers().keySet().stream().noneMatch(CONTENT_TYPE::equalsIgnoreCase)) {
			builder.header(CONTENT_TYPE, JSON);
		}
		builder.method(request.method(), body == null
				? HttpRequest.BodyPublishers.noBody()
				: HttpRequest.BodyPublishers.ofString(body));
		try {
			java.net.http.HttpResponse<String> response = httpClient.send(builder.build(), BodyHandlers.ofString());
			return new Response(response.statusCode(), response.body() == null ? "" : response.body());
		} catch (IOException e) {
			throw new IllegalStateException("Was not able to reach " + base + ": " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while calling " + base + ".", e);
		}
	}
}
