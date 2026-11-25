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
	private String basepath = "/";

	/**
	 * Root URL of the server to be used in the ngsild-subscription callbacks
	 */
	private String serverHost;

	/**
	 * Whether values that would otherwise contain NGSI-LD's optional "Forbidden Characters"
	 * ({@code < > " ' = ; ( )}, ETSI GS CIM 009 clause 4.6.4) are escaped before being sent to the
	 * broker (and unescaped when read back). Rejecting those characters is optional per spec -
	 * Orion-LD/Scorpio don't enforce it, some brokers do. Default true; set to false for a broker
	 * (or existing data) that expects the plain, unescaped value.
	 * Global, not broker-only: data migrated from a broker may still be escaped.
	 */
	private boolean escapeForbiddenCharacters = true;

	/**
	 * When a {@code .regex} filter's value matches the "not equal" idiom
	 * {@code ^(?!X$).*$}, translate it into NGSI-LD's native {@code !=} comparison
	 * instead of forwarding {@code ~=} (pattern match) to the broker. Not every
	 * broker's regex engine supports negative lookahead (e.g. Scorpio's
	 * PostgreSQL-backed POSIX regex doesn't), so this optimization keeps the
	 * common "not equal" case portable. Default true.
	 * Global, not broker-only: PostgreSQL's POSIX regex has no lookahead either.
	 */
	private boolean optimizeRegexNotEquals = true;
}
