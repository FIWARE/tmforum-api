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
	 * {@code ^(?!X$).*$} and X is a literal, normalize it into a NOT_EQUALS
	 * comparison instead of carrying the pattern to the backend. Not every regex
	 * engine supports negative lookahead, and a pattern is read by each backend's
	 * own engine, so normalizing keeps the common "not equal" case portable and
	 * backend-neutral. Default true. X holding a regex metacharacter is a pattern
	 * rather than a literal and stays one whatever this says.
	 */
	private boolean optimizeRegexNotEquals = true;
}
