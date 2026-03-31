package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.net.URI;

/**
 * A base class used to define the invariant characteristics and behavior of an API.
 * Extends SoftwareResourceSpecification with API-specific attributes like protocol type,
 * authentication type, and URL endpoints.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = ApiSpecification.TYPE_API_SPECIFICATION)
public class ApiSpecification extends SoftwareResourceSpecification {

	public static final String TYPE_API_SPECIFICATION = "api-specification";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "apiProtocolType") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "apiProtocolType") }))
	private String apiProtocolType;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "authenticationType") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "authenticationType") }))
	private String authenticationType;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "externalSchema") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "externalSchema", targetClass = URI.class) }))
	private URI externalSchema;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "externalUrl") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "externalUrl", targetClass = URI.class) }))
	private URI externalUrl;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "internalSchema") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "internalSchema", targetClass = URI.class) }))
	private URI internalSchema;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "internalUrl") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "internalUrl", targetClass = URI.class) }))
	private URI internalUrl;

	/**
	 * Create a new ApiSpecification.
	 *
	 * @param id the entity id
	 */
	public ApiSpecification(String id) {
		super(TYPE_API_SPECIFICATION, id);
	}
}
