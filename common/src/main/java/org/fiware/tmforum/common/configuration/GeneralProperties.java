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
	 * Tenant to be used by the tmforum api.
	 */
	private String tenant = null;

	/**
	 * Character used in target NGSI-LD broker for making
	 * or queries in a specific value
	 */
	private String ngsildOrQueryValue=",";

	/**
	 * Character used in target NGSI-LD broker for making
	 * or queries between multiple parameters
	 */
	private String ngsildOrQueryKey=",";

	/**
	 * Whether to enclose queries using brackets or not
	 */
	private Boolean encloseQuery=false;

	/**
	 * Whether to include the attribute name in each list object or not
	 * E.g. (status==\"Active\",\"Started\") or (status==\"Active\",status==\"Started\")
	 */
	private Boolean includeAttributeInList=false;

	/**
	 * Wether to use a dot as seperator for the attribute paths in queries or not(and the ["path-part"] instead)
	 * E.g.:
	 * true: entity.attribute.subAttribute
	 * false: entity[attribute][subAttribute]
	 */
	private Boolean useDotSeperator=true;

	/**
	 * When true, every attribute of an update replaces the stored one (PATCH /attrs, deleting the stored instances
	 * the update does not carry with an NGSI-LD Null), so that e.g. a list ends up with exactly the items of the
	 * update. Required for Scorpio 6.x and other brokers that keep the existing instances when appending. When false
	 * (default, Orion-LD), the attributes of an update are appended as they are (POST /attrs).
	 */
	private boolean replaceOnUpdate = false;

	/**
	 * Name of the response header the target NGSI-LD broker uses to report the total number of
	 * entities matching a query. Defaults to the header name defined by the NGSI-LD standard; brokers
	 * that deviate from the standard can override this property per profile. Set to null/empty to
	 * disable count reporting - in that case, pagination degrades gracefully (no X-Total-Count, no
	 * exact next/last, status always 200).
	 */
	private String countHeader = "NGSILD-Results-Count";

	/**
	 * Whether values that would otherwise contain NGSI-LD's optional "Forbidden Characters"
	 * ({@code < > " ' = ; ( )}, ETSI GS CIM 009 clause 4.6.4) are escaped before being sent to the
	 * broker (and unescaped when read back). Rejecting those characters is optional per spec -
	 * Orion-LD/Scorpio don't enforce it, some brokers do. Default true; set to false for a broker
	 * (or existing data) that expects the plain, unescaped value.
	 */
	private boolean escapeForbiddenCharacters = true;

	/**
	 * When a {@code .regex} filter's value matches the "not equal" idiom
	 * {@code ^(?!X$).*$}, translate it into NGSI-LD's native {@code !=} comparison
	 * instead of forwarding {@code ~=} (pattern match) to the broker. Not every
	 * broker's regex engine supports negative lookahead (e.g. Scorpio's
	 * PostgreSQL-backed POSIX regex doesn't), so this optimization keeps the
	 * common "not equal" case portable. Default true.
	 */
	private boolean optimizeRegexNotEquals = true;
}
