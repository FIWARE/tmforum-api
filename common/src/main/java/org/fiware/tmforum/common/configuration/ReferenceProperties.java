package org.fiware.tmforum.common.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.List;

/**
 * Operator allow-list of references held by other deployments. Matching references are not validated locally.
 * Both lists default to empty, so every reference is local and validated strictly.
 */
@ConfigurationProperties(ReferenceProperties.PREFIX)
@Data
public class ReferenceProperties {

	/**
	 * Configuration prefix of the reference properties.
	 */
	public static final String PREFIX = "references";

	/**
	 * Id prefixes of external references. Must be deployment-specific, e.g. {@code urn:ngsi-ld:category:partner-a-},
	 * since a prefix overlapping local ids switches validation off for them.
	 */
	private List<String> externalIdPrefixes = List.of();

	/**
	 * Href prefixes of external references, e.g. {@code https://partner-a.example.org/}.
	 */
	private List<String> externalHrefPrefixes = List.of();
}
