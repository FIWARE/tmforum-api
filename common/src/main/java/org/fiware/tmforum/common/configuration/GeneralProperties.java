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

	/**
	 * Character used in target NGSI-LD broker for making
	 * or queries in a specific value
	 */
	private String ngsildOrQueryValue;

	/**
	 * Character used in target NGSI-LD broker for making
	 * or queries between mutiple parameters
	 */
	private String ngsildOrQueryKey;

	/**
	 * Whether to enclose queries using bracets or not
	 */
	private Boolean encloseQuery;
}
