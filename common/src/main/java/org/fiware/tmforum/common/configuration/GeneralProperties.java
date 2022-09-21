package org.fiware.tmforum.common.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.net.URL;

/**
 * General properties to be used for the applications.
 */
@ConfigurationProperties("general")
@Data
public class GeneralProperties {

	/**
	 * ContextUrl for the service to use.
	 */
	private URL contextUrl;

	/**
	 * Base path for the controllers to be deployed at. If nothing is set, the project-individual defaults will be used
	 */
	private String basePath;

	/**
	 * Tenant to be used by the tmforum api.
	 */
	private String tenant = null;

}
