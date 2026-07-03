package org.fiware.tmforum.common.repository;

import io.micronaut.http.HttpResponse;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TmForumRepositoryTest {

	private GeneralProperties properties;
	private TmForumRepository repository;

	@BeforeEach
	public void setUp() {
		properties = new GeneralProperties();
		repository = new TmForumRepository(properties, null, null, null, null, null);
	}

	@Test
	public void extractsTheConfiguredHeader() {
		properties.setCountHeader("NGSILD-Results-Count");
		HttpResponse<Object> response = HttpResponse.ok().header("NGSILD-Results-Count", "42");

		assertEquals(42, repository.extractTotalCount(response));
	}

	@Test
	public void returnsNullWhenNoHeaderIsConfigured() {
		HttpResponse<Object> response = HttpResponse.ok().header("NGSILD-Results-Count", "42");

		assertNull(repository.extractTotalCount(response),
				"Without a configured header name, the total is unknown even if the broker sent one.");
	}

	@Test
	public void returnsNullWhenTheBrokerDidNotSendTheHeader() {
		properties.setCountHeader("NGSILD-Results-Count");
		HttpResponse<Object> response = HttpResponse.ok();

		assertNull(repository.extractTotalCount(response));
	}

	@Test
	public void returnsNullWhenTheHeaderIsNotAnInteger() {
		properties.setCountHeader("NGSILD-Results-Count");
		HttpResponse<Object> response = HttpResponse.ok().header("NGSILD-Results-Count", "not-a-number");

		assertNull(repository.extractTotalCount(response),
				"A malformed header must degrade to unknown, not fail the request.");
	}
}
