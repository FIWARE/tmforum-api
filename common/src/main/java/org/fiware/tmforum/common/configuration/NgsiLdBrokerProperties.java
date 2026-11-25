package org.fiware.tmforum.common.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

/**
 * NGSI-LD broker dialect under the {@code general} keys; ignored by the PostgreSQL backend.
 */
@ConfigurationProperties("general")
@Data
public class NgsiLdBrokerProperties {

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
	 * Whether to include the attribute name in each list object or not, joined with {@link #ngsildOrQueryKey} then
	 * E.g. (status==\"Active\",\"Started\") or (status==\"Active\"|status==\"Started\")
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
}
