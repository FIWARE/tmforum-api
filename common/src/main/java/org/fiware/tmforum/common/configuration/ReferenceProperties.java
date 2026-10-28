package org.fiware.tmforum.common.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.List;

/**
 * Identifies the deployments reference validation trusts. A reference is external only if its href names one of
 * {@code references.external-hosts} and none of this deployment's own hosts.
 */
@ConfigurationProperties(ReferenceProperties.PREFIX)
@Data
public class ReferenceProperties {

	/**
	 * Configuration prefix of the reference properties.
	 */
	public static final String PREFIX = "references";

	/**
	 * Host names of the deployments whose entities may be referenced without being validated, e.g. a peer TMForum
	 * deployment. Empty by default, so every reference is validated against local storage.
	 */
	private List<String> externalHosts = List.of();

	/**
	 * Host names this deployment is reachable under, besides the host of {@code general.serverHost}, e.g. the
	 * public host behind an ingress. They are never treated as external.
	 */
	private List<String> localHosts = List.of();
}
