package org.fiware.tmforum.migration.writer;

import io.github.wistefan.mapping.JavaObjectMapper;
import io.micronaut.context.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;

import java.util.Collections;

/**
 * Tanslates the Pojos to Entities, using the new library version and stores them in a target broker.
 */
@Slf4j
public class UpdateWriter {

	private volatile ApplicationContext applicationContext;

	public <T> void writeUpdate(Object theObject) throws Exception {
		if (applicationContext == null) {
			applicationContext = ApplicationContext.builder()
					.properties(Collections.singletonMap("micronaut.http.services.ngsi.url", "http://localhost:1026"))
					.start();
		} else {
			if (!applicationContext.isRunning()) {
				applicationContext = ApplicationContext.builder()
						.properties(Collections.singletonMap("micronaut.http.services.ngsi.url", "http://localhost:1026"))
						.start();
			}
		}
		EntitiesApiClient entitiesApiClient = applicationContext.getBean(EntitiesApiClient.class);
		GeneralProperties generalProperties = applicationContext.getBean(GeneralProperties.class);

		JavaObjectMapper javaObjectMapper = applicationContext.getBean(JavaObjectMapper.class);
		EntityVO e = javaObjectMapper.toEntityVO(theObject);
		entitiesApiClient.createEntity(e, generalProperties.getTenant()).block();
	}
}
