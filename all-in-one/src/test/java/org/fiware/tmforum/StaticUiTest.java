package org.fiware.tmforum;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tmf-ui (module {@code ui}) is served from the classpath at /ui/, and only when asked to:
 * it is off unless TMF_UI_ENABLED=true. No broker is needed - nothing here reaches the API.
 */
public class StaticUiTest {

	private static String get(HttpClient client, String path) {
		HttpResponse<String> response = client.toBlocking().exchange(HttpRequest.GET(path), String.class);
		assertEquals(HttpStatus.OK, response.getStatus(), path);
		return response.body();
	}

	@Nested
	@MicronautTest(environments = {"in-memory", "orion-ld"})
	@Property(name = "apiExtension.enabled", value = "false")
	@Property(name = "micronaut.server.port", value = "-1")
	@Property(name = "micronaut.router.static-resources.ui.enabled", value = "true")
	class Enabled {

		@Inject
		@Client("/")
		HttpClient client;

		@Test
		void servesThePage() {
			assertTrue(get(client, "/ui/").contains("<title>TMF UI</title>"));
		}

		@Test
		void servesTheScriptsThePageLoads() {
			for (String asset : new String[]{"app.js", "refs.js", "catalog.js", "styles.css"}) {
				get(client, "/ui/" + asset);
			}
		}

		// The jar's config.json is what tells the page to call its own origin rather than
		// the dev server's proxy, which does not exist here.
		@Test
		void tellsThePageToUseItsOwnOrigin() {
			assertTrue(get(client, "/ui/config.json").replaceAll("\\s", "").contains("\"sameOrigin\":true"));
		}
	}

	@Nested
	@MicronautTest(environments = {"in-memory", "orion-ld"})
	@Property(name = "apiExtension.enabled", value = "false")
	@Property(name = "micronaut.server.port", value = "-1")
	class DisabledByDefault {

		@Inject
		@Client("/")
		HttpClient client;

		@Test
		void servesNothing() {
			HttpClientResponseException e = assertThrows(HttpClientResponseException.class,
					() -> client.toBlocking().exchange(HttpRequest.GET("/ui/"), String.class));
			assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
		}
	}
}
