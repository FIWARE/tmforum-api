package org.fiware.tmforum.common.filter;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.exceptions.HttpClientException;
import org.fiware.tmforum.common.exception.DeletionException;
import org.fiware.tmforum.common.exception.DeletionExceptionReason;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.common.exception.SchemaValidationException;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.StubBrokerController;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Test-only endpoints for {@link ServerWiringRegressionTest}: a paginated list, and one route per exception the
 * registered {@code ExceptionHandler}s map.
 */
@Requires(property = StubBrokerController.SPEC_NAME_PROPERTY, value = ServerWiringRegressionTest.SPEC_NAME)
@Controller(WiringTestController.BASE_PATH)
public class WiringTestController {

	public static final String BASE_PATH = "/wiring-test";
	public static final String LIST_PATH = "/list";
	public static final String THROW_PATH = "/throw/";
	public static final List<String> LIST_ITEMS = List.of("a", "b");
	public static final int LIMIT = 2;

	private static final String MESSAGE = "wiring-test";
	private static final String UNPARSABLE_DATE = "not-a-date";
	private static final int UNPARSABLE_DATE_ERROR_INDEX = 0;

	/** Exceptions by the name used in the {@link #THROW_PATH} route. */
	public static final Map<String, Supplier<RuntimeException>> EXCEPTIONS = Map.ofEntries(
			Map.entry("tmforum-conflict", () -> new TmForumException(MESSAGE, TmForumExceptionReason.CONFLICT)),
			Map.entry("tmforum-invalid-data", () -> new TmForumException(MESSAGE, TmForumExceptionReason.INVALID_DATA)),
			Map.entry("tmforum-invalid-relationship",
					() -> new TmForumException(MESSAGE, TmForumExceptionReason.INVALID_RELATIONSHIP)),
			Map.entry("tmforum-not-found", () -> new TmForumException(MESSAGE, TmForumExceptionReason.NOT_FOUND)),
			Map.entry("tmforum-unknown", () -> new TmForumException(MESSAGE, TmForumExceptionReason.UNKNOWN)),
			Map.entry("deletion-not-found", () -> new DeletionException(MESSAGE, DeletionExceptionReason.NOT_FOUND)),
			Map.entry("deletion-unknown", () -> new DeletionException(MESSAGE, DeletionExceptionReason.UNKNOWN)),
			Map.entry("query", () -> new QueryException(MESSAGE)),
			Map.entry("schema-validation", () -> new SchemaValidationException(List.of(MESSAGE), MESSAGE)),
			Map.entry("http-client", () -> new HttpClientException(MESSAGE)),
			Map.entry("date-time-parse",
					() -> new DateTimeParseException(MESSAGE, UNPARSABLE_DATE, UNPARSABLE_DATE_ERROR_INDEX)),
			Map.entry("unexpected", () -> new IllegalStateException(MESSAGE)));

	// sets the same request attributes AbstractApiController#list hands to the PaginationFilter
	@Get(LIST_PATH)
	public List<String> list(HttpRequest<?> request) {
		request.setAttribute(PaginationFilter.OFFSET_ATTR, 0)
				.setAttribute(PaginationFilter.LIMIT_ATTR, LIMIT)
				.setAttribute(PaginationFilter.RETURNED_COUNT_ATTR, LIST_ITEMS.size());
		return LIST_ITEMS;
	}

	@Get(THROW_PATH + "{exception}")
	public String fail(String exception) {
		throw EXCEPTIONS.get(exception).get();
	}
}
