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
	 * When true, updateDomainEntity uses batchEntityUpsert replace (read-merge-write) instead of PATCH /attrs.
	 * Required for Scorpio 6.x which appends to array attributes on PATCH. Default false (Orion-LD).
	 */
	private boolean replaceOnUpdate = false;

	/**
	 * Name of the response header the target NGSI-LD broker uses to report the total number of
	 * entities matching a query (e.g. "NGSILD-Results-Count" for Scorpio, "Fiware-Total-Count" for
	 * Orion-LD). Null if not configured for the active broker profile - in that case, pagination
	 * degrades gracefully (no X-Total-Count, no exact next/last, status always 200).
	 */
	private String countHeader = null;
}
