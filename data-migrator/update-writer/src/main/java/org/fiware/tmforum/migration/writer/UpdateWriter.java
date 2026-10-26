package org.fiware.tmforum.migration.writer;

import tools.jackson.databind.ObjectMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;

import java.util.Map;

/**
 * Tanslates the Pojos to Entities, using the new library version and stores them in a target broker.
 */
@Slf4j
public class UpdateWriter {

	private static final String NGSI_URL_PROPERTY = "micronaut.http.services.ngsi.url";
	private static final String SERIALIZATION_INCLUSION_PROPERTY = "jackson.serialization-inclusion";
	// Micronaut's NON_EMPTY default would drop empty values and lists of migrated entities
	private static final String SERIALIZATION_INCLUSION = "non_null";
	private static final String WRITE_DATES_AS_TIMESTAMPS_PROPERTY = "jackson.date-time-features.write-dates-as-timestamps";

	private volatile ApplicationContext applicationContext;

	private String brokerAddress;

	public <T> void writeUpdate(Object theObject) throws Exception {
		if (applicationContext == null || !applicationContext.isRunning()) {
			applicationContext = startContext();
		}
		EntitiesApiClient entitiesApiClient = applicationContext.getBean(EntitiesApiClient.class);
		GeneralProperties generalProperties = applicationContext.getBean(GeneralProperties.class);
		ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);

		JavaObjectMapper javaObjectMapper = applicationContext.getBean(JavaObjectMapper.class);


		EntityVO e = javaObjectMapper.toEntityVO(theObject);
		try {
			entitiesApiClient.createEntity(e, generalProperties.getTenant()).block();
		} catch (HttpClientResponseException cre) {
			System.out.println("Ex " + cre.getResponse().getBody(String.class));
			System.out.println("The entity " + objectMapper.writeValueAsString(e));
		}
	}

	// Set explicitly, since the context does not reliably see the migrator's application.yaml.
	private ApplicationContext startContext() {
		return ApplicationContext.builder()
				.classLoader(Thread.currentThread().getContextClassLoader())
				.properties(Map.of(
						NGSI_URL_PROPERTY, brokerAddress,
						SERIALIZATION_INCLUSION_PROPERTY, SERIALIZATION_INCLUSION,
						WRITE_DATES_AS_TIMESTAMPS_PROPERTY, false))
				.build()
				.start();
	}

	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}
}
