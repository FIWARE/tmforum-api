package org.fiware.tmforum.productcatalog;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TMForum hub callbacks of the event ITs: records every event POSTed to {@code /test-hubs/{hub}}.
 */
@Controller(RecordingHubController.PATH)
@Requires(property = RecordingHubController.ENABLED_PROPERTY, value = "true")
public class RecordingHubController {

	static final String PATH = "/test-hubs";
	static final String ENABLED_PROPERTY = "test.recording-hubs.enabled";

	private final Map<String, List<Map<String, Object>>> received = new ConcurrentHashMap<>();

	@Post("/{hub}")
	public HttpResponse<Void> receive(@PathVariable String hub, @Body Map<String, Object> event) {
		received.computeIfAbsent(hub, h -> new CopyOnWriteArrayList<>()).add(event);
		return HttpResponse.noContent();
	}

	List<Map<String, Object>> received(String hub) {
		return received.getOrDefault(hub, List.of());
	}

	void clear() {
		received.clear();
	}
}
