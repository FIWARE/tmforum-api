package org.fiware.tmforum.softwaremanagement;

import io.github.wistefan.mapping.MappingException;
import org.fiware.softwaremanagement.model.*;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects for the Software Management API (TMF730).
 * Maps Resource and ResourceSpecification domain entities to and from their generated VO counterparts.
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public abstract class TMForumMapper extends BaseMapper {

	// --- Resource mappings ---

	/**
	 * Map a {@link ResourceCreateVO} with a generated id to a {@link ResourceVO}.
	 *
	 * @param resourceCreateVO the resource creation value object
	 * @param id               the generated URI id
	 * @return the mapped resource value object
	 */
	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ResourceVO map(ResourceCreateVO resourceCreateVO, URI id);

	/**
	 * Map a {@link Resource} domain entity to a {@link ResourceVO}.
	 *
	 * @param resource the resource domain entity
	 * @return the mapped resource value object
	 */
	public abstract ResourceVO map(Resource resource);

	/**
	 * Map a {@link ResourceVO} to a {@link Resource} domain entity.
	 *
	 * @param resourceVO the resource value object
	 * @return the mapped resource domain entity
	 */
	public abstract Resource map(ResourceVO resourceVO);

	/**
	 * Map a {@link ResourceUpdateVO} with its id to a {@link Resource} domain entity.
	 *
	 * @param resourceUpdateVO the resource update value object
	 * @param id               the resource id
	 * @return the mapped resource domain entity
	 */
	@Mapping(target = "id", source = "id")
	public abstract Resource map(ResourceUpdateVO resourceUpdateVO, String id);

	// --- ResourceSpecification mappings ---

	/**
	 * Map a {@link ResourceSpecificationCreateVO} with a generated id to a {@link ResourceSpecificationVO}.
	 *
	 * @param resourceSpecificationCreateVO the resource specification creation value object
	 * @param id                            the generated URI id
	 * @return the mapped resource specification value object
	 */
	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ResourceSpecificationVO map(ResourceSpecificationCreateVO resourceSpecificationCreateVO, URI id);

	/**
	 * Map a {@link ResourceSpecification} domain entity to a {@link ResourceSpecificationVO}.
	 *
	 * @param resourceSpecification the resource specification domain entity
	 * @return the mapped resource specification value object
	 */
	public abstract ResourceSpecificationVO map(ResourceSpecification resourceSpecification);

	/**
	 * Map a {@link ResourceSpecificationVO} to a {@link ResourceSpecification} domain entity.
	 *
	 * @param resourceSpecificationVO the resource specification value object
	 * @return the mapped resource specification domain entity
	 */
	public abstract ResourceSpecification map(ResourceSpecificationVO resourceSpecificationVO);

	/**
	 * Map a {@link ResourceSpecificationUpdateVO} with its id to a {@link ResourceSpecification} domain entity.
	 *
	 * @param resourceSpecificationUpdateVO the resource specification update value object
	 * @param id                            the resource specification id
	 * @return the mapped resource specification domain entity
	 */
	@Mapping(target = "id", source = "id")
	public abstract ResourceSpecification map(ResourceSpecificationUpdateVO resourceSpecificationUpdateVO, String id);

	// --- EventSubscription mapping ---

	/**
	 * Map a {@link TMForumSubscription} to an {@link EventSubscriptionVO}.
	 *
	 * @param subscription the TM Forum subscription
	 * @return the mapped event subscription value object
	 */
	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	// --- Note mappings ---

	/**
	 * Map a {@link Note} domain entity to a {@link NoteVO}.
	 *
	 * @param note the note domain entity
	 * @return the mapped note value object
	 */
	@Mapping(target = "id", source = "tmfId")
	public abstract NoteVO map(Note note);

	/**
	 * Map a {@link NoteVO} to a {@link Note} domain entity.
	 *
	 * @param noteVO the note value object
	 * @return the mapped note domain entity
	 */
	@Mapping(target = "tmfId", source = "id")
	public abstract Note map(NoteVO noteVO);

	// --- Feature mappings ---

	/**
	 * Map a {@link Feature} domain entity to a {@link FeatureVO}.
	 *
	 * @param feature the feature domain entity
	 * @return the mapped feature value object
	 */
	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureVO map(Feature feature);

	/**
	 * Map a {@link FeatureVO} to a {@link Feature} domain entity.
	 *
	 * @param featureVO the feature value object
	 * @return the mapped feature domain entity
	 */
	@Mapping(target = "tmfId", source = "id")
	// ignore them, since they are not present in the current api version
	@Mapping(target = "atSchemaLocation", ignore = true)
	@Mapping(target = "atBaseType", ignore = true)
	@Mapping(target = "atType", ignore = true)
	public abstract Feature map(FeatureVO featureVO);

	// --- FeatureRelationship mappings ---

	/**
	 * Map a {@link FeatureRelationship} domain entity to a {@link FeatureRelationshipVO}.
	 *
	 * @param feature the feature relationship domain entity
	 * @return the mapped feature relationship value object
	 */
	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureRelationshipVO map(FeatureRelationship feature);

	/**
	 * Map a {@link FeatureRelationshipVO} to a {@link FeatureRelationship} domain entity.
	 *
	 * @param featureVO the feature relationship value object
	 * @return the mapped feature relationship domain entity
	 */
	@Mapping(target = "tmfId", source = "id")
	public abstract FeatureRelationship map(FeatureRelationshipVO featureVO);

	// --- Characteristic mappings ---

	/**
	 * Map a {@link CharacteristicVO} to a {@link Characteristic} domain entity.
	 *
	 * @param characteristicVO the characteristic value object
	 * @return the mapped characteristic domain entity
	 */
	@Mapping(target = "tmfValue", source = "value")
	@Mapping(target = "tmfId", source = "id")
	public abstract Characteristic map(CharacteristicVO characteristicVO);

	/**
	 * Map a {@link Characteristic} domain entity to a {@link CharacteristicVO}.
	 *
	 * @param characteristic the characteristic domain entity
	 * @return the mapped characteristic value object
	 */
	@Mapping(target = "value", source = "tmfValue")
	@Mapping(target = "id", source = "tmfId")
	public abstract CharacteristicVO map(Characteristic characteristic);

	// --- CharacteristicRelationship mappings ---

	/**
	 * Map a {@link CharacteristicRelationshipVO} to a {@link CharacteristicRelationship} domain entity.
	 *
	 * @param characteristicRelationshipVO the characteristic relationship value object
	 * @return the mapped characteristic relationship domain entity
	 */
	@Mapping(target = "tmfId", source = "id")
	public abstract CharacteristicRelationship map(CharacteristicRelationshipVO characteristicRelationshipVO);

	/**
	 * Map a {@link CharacteristicRelationship} domain entity to a {@link CharacteristicRelationshipVO}.
	 *
	 * @param characteristicRelationship the characteristic relationship domain entity
	 * @return the mapped characteristic relationship value object
	 */
	@Mapping(target = "id", source = "tmfId")
	public abstract CharacteristicRelationshipVO map(CharacteristicRelationship characteristicRelationship);

	// --- AttachmentRefOrValue mappings ---

	/**
	 * Map an {@link AttachmentRefOrValueVO} to an {@link AttachmentRefOrValue} domain entity.
	 *
	 * @param attachmentRefOrValueVO the attachment ref or value VO
	 * @return the mapped attachment ref or value domain entity
	 */
	@Mapping(target = "tmfId", source = "id")
	public abstract AttachmentRefOrValue map(AttachmentRefOrValueVO attachmentRefOrValueVO);

	/**
	 * Map an {@link AttachmentRefOrValue} domain entity to an {@link AttachmentRefOrValueVO}.
	 *
	 * @param attachmentRefOrValue the attachment ref or value domain entity
	 * @return the mapped attachment ref or value VO
	 */
	@Mapping(target = "id", source = "tmfId")
	public abstract AttachmentRefOrValueVO map(AttachmentRefOrValue attachmentRefOrValue);

	// --- FeatureSpecification mappings ---

	/**
	 * Map a {@link FeatureSpecificationVO} to a {@link FeatureSpecification} domain entity.
	 *
	 * @param featureSpecificationVO the feature specification value object
	 * @return the mapped feature specification domain entity
	 */
	@Mapping(target = "tmfId", source = "id")
	public abstract FeatureSpecification map(FeatureSpecificationVO featureSpecificationVO);

	/**
	 * Map a {@link FeatureSpecification} domain entity to a {@link FeatureSpecificationVO}.
	 *
	 * @param featureSpecification the feature specification domain entity
	 * @return the mapped feature specification value object
	 */
	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureSpecificationVO map(FeatureSpecification featureSpecification);

	// --- FeatureSpecificationCharacteristic mappings ---

	/**
	 * Map a {@link FeatureSpecificationCharacteristicVO} to a {@link FeatureSpecificationCharacteristic} domain entity.
	 *
	 * @param featureSpecificationVO the feature specification characteristic value object
	 * @return the mapped feature specification characteristic domain entity
	 */
	@Mapping(target = "tmfId", source = "id")
	public abstract FeatureSpecificationCharacteristic map(FeatureSpecificationCharacteristicVO featureSpecificationVO);

	/**
	 * Map a {@link FeatureSpecificationCharacteristic} domain entity to a {@link FeatureSpecificationCharacteristicVO}.
	 *
	 * @param featureSpecification the feature specification characteristic domain entity
	 * @return the mapped feature specification characteristic value object
	 */
	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureSpecificationCharacteristicVO map(FeatureSpecificationCharacteristic featureSpecification);

	// --- FeatureSpecificationCharacteristicRelationship mappings ---

	/**
	 * Map a {@link FeatureSpecificationCharacteristicRelationshipVO} to a
	 * {@link FeatureSpecificationCharacteristicRelationship} domain entity.
	 *
	 * @param featureSpecificationVO the feature specification characteristic relationship value object
	 * @return the mapped feature specification characteristic relationship domain entity
	 */
	@Mapping(target = "tmfId", source = "id")
	public abstract FeatureSpecificationCharacteristicRelationship map(
			FeatureSpecificationCharacteristicRelationshipVO featureSpecificationVO);

	/**
	 * Map a {@link FeatureSpecificationCharacteristicRelationship} domain entity to a
	 * {@link FeatureSpecificationCharacteristicRelationshipVO}.
	 *
	 * @param featureSpecification the feature specification characteristic relationship domain entity
	 * @return the mapped feature specification characteristic relationship value object
	 */
	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureSpecificationCharacteristicRelationshipVO map(
			FeatureSpecificationCharacteristicRelationship featureSpecification);

	// --- FeatureSpecificationRelationship mappings ---

	/**
	 * Map a {@link FeatureSpecificationRelationshipVO} to a {@link FeatureSpecificationRelationship} domain entity.
	 *
	 * @param featureSpecificationVO the feature specification relationship value object
	 * @return the mapped feature specification relationship domain entity
	 */
	public abstract FeatureSpecificationRelationship map(FeatureSpecificationRelationshipVO featureSpecificationVO);

	/**
	 * Map a {@link FeatureSpecificationRelationship} domain entity to a {@link FeatureSpecificationRelationshipVO}.
	 *
	 * @param featureSpecification the feature specification relationship domain entity
	 * @return the mapped feature specification relationship value object
	 */
	public abstract FeatureSpecificationRelationshipVO map(FeatureSpecificationRelationship featureSpecification);

	// --- CharacteristicValue mappings ---

	/**
	 * Map a {@link CharacteristicValueSpecificationVO} to a {@link CharacteristicValue} domain entity.
	 *
	 * @param characteristicVO the characteristic value specification value object
	 * @return the mapped characteristic value domain entity
	 */
	@Mapping(target = "tmfValue", source = "value")
	public abstract CharacteristicValue map(CharacteristicValueSpecificationVO characteristicVO);

	/**
	 * Map a {@link CharacteristicValue} domain entity to a {@link CharacteristicValueSpecificationVO}.
	 *
	 * @param characteristic the characteristic value domain entity
	 * @return the mapped characteristic value specification value object
	 */
	@Mapping(target = "value", source = "tmfValue")
	public abstract CharacteristicValueSpecificationVO map(CharacteristicValue characteristic);

	// --- ResourceSpecificationRef converters ---

	/**
	 * Convert a string id to a {@link ResourceSpecificationRef}.
	 *
	 * @param id the resource specification id string
	 * @return the constructed reference, or null if the input is null
	 */
	public ResourceSpecificationRef mapFromResourceSpecId(String id) {
		if (id == null) {
			return null;
		}
		return new ResourceSpecificationRef(id);
	}

	/**
	 * Convert a {@link ResourceSpecificationRef} to its string id.
	 *
	 * @param resourceSpecificationRef the resource specification reference
	 * @return the string id, or null if the input is null
	 */
	public String mapFromResourceSpecificationRef(ResourceSpecificationRef resourceSpecificationRef) {
		if (resourceSpecificationRef == null) {
			return null;
		}
		return resourceSpecificationRef.getEntityId().toString();
	}

	// --- URL/String converters ---

	/**
	 * Convert a string value to a {@link URL}.
	 *
	 * @param value the string value
	 * @return the converted URL, or null if the input is null
	 */
	public URL map(String value) {
		if (value == null) {
			return null;
		}
		try {
			return new URL(value);
		} catch (MalformedURLException e) {
			throw new MappingException(String.format("%s is not a URL.", value), e);
		}
	}

	/**
	 * Convert a {@link URL} to a string value.
	 *
	 * @param value the URL value
	 * @return the string representation, or null if the input is null
	 */
	public String map(URL value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	/**
	 * Convert a {@link URL} to a {@link URI}.
	 *
	 * @param value the URL value
	 * @return the converted URI, or null if the input is null
	 */
	public URI mapFromURL(URL value) {
		if (value == null) {
			return null;
		}
		try {
			return value.toURI();
		} catch (URISyntaxException e) {
			throw new MappingException(String.format("Value %s is not an URI.", value), e);
		}
	}

	/**
	 * Convert a string value to a {@link URI}.
	 *
	 * @param value the string value
	 * @return the converted URI, or null if the input is null
	 */
	public URI mapToURI(String value) {
		if (value == null) {
			return null;
		}
		return URI.create(value);
	}

	/**
	 * Convert a {@link URI} to a string value.
	 *
	 * @param value the URI value
	 * @return the string representation, or null if the input is null
	 */
	public String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}
}
