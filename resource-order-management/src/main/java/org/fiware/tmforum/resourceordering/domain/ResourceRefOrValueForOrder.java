package org.fiware.tmforum.resourceordering.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.Entity;

/**
 * A resource ref or value as used within a resource order item.
 * This is a simplified representation that holds the resource data inline.
 */
@Data
public class ResourceRefOrValueForOrder extends Entity {

	private String tmfId;
	private String href;
	private String name;
	private String description;
	private String category;
	private String resourceVersion;
}
