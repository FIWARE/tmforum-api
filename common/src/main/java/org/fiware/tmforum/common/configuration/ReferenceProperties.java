package org.fiware.tmforum.common.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.List;

/**
 * Identifies this deployment for reference validation. A reference whose href names any other host is external.
 */
@ConfigurationProperties(ReferenceProperties.PREFIX)
@Data
public class ReferenceProperties {

	/**
	 * Configuration prefix of the reference properties.
	 */
	public static final String PREFIX = "references";

	/**
	 * Host names this deployment is reachable under, besides the host of {@code general.serverHost}, e.g. the
	 * public host behind an ingress.
	 */
	private List<String> localHosts = List.of();
}
