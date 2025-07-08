package org.fiware.tmforum.migration.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wistefan.mapping.AdditionalPropertyMixin;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.AdditionalPropertyListVO;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.ObjectMapperEventListener;

import java.util.Collections;

/**
 * Tanslates the Pojos to Entities, using the new library version and stores them in a target broker.
 */
@Slf4j
public class UpdateWriter {

	private volatile ApplicationContext applicationContext;

	private String brokerAddress;

	public <T> void writeUpdate(Object theObject) throws Exception {
		if (applicationContext == null) {
			applicationContext = ApplicationContext.builder()
					.classLoader(Thread.currentThread().getContextClassLoader())
					.properties(Collections.singletonMap("micronaut.http.services.ngsi.url", brokerAddress))
					.build()
					.start();
		} else {
			if (!applicationContext.isRunning()) {
				applicationContext = ApplicationContext.builder()
						.classLoader(Thread.currentThread().getContextClassLoader())
						.properties(Collections.singletonMap("micronaut.http.services.ngsi.url", brokerAddress))
						.build()
						.start();
			}
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

	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}
}
