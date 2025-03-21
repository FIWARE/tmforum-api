package org.fiware.tmforum.resourceinventory;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourceinventory.model.*;
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
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public abstract class TMForumMapper extends BaseMapper {

	// resource catalog

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ResourceVO map(ResourceCreateVO resourceCreateVO, URI id);

	public abstract ResourceVO map(Resource resource);

	public abstract Resource map(ResourceVO resourceVO);

	@Mapping(target = "id", source = "id")
	public abstract Resource map(ResourceUpdateVO resourceUpdateVO, String id);

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	@Mapping(target = "id", source = "noteId")
	public abstract NoteVO map(Note note);

	@Mapping(target = "noteId", source = "id")
	public abstract Note map(NoteVO noteVO);

	@Mapping(target = "id", source = "featureId")
	public abstract FeatureVO map(Feature feature);

	@Mapping(target = "featureId", source = "id")
	public abstract Feature map(FeatureVO featureVO);

	@Mapping(target = "characteristicValue", source = "value")
	@Mapping(target = "characteristicId", source = "id")
	public abstract Characteristic map(CharacteristicVO characteristicVO);

	@Mapping(target = "value", source = "characteristicValue")
	@Mapping(target = "id", source = "characteristicId")
	public abstract CharacteristicVO map(Characteristic characteristic);

	@Mapping(target = "characteristicRelationId", source = "id")
	public abstract CharacteristicRelationship map(CharacteristicRelationshipVO characteristicRelationshipVO);

	@Mapping(target = "id", source = "characteristicRelationId")
	public abstract CharacteristicRelationshipVO map(CharacteristicRelationship characteristicRelationship);

	@Mapping(target = "attachementId", source = "id")
	public abstract AttachmentRefOrValue map(AttachmentRefOrValueVO attachmentRefOrValueVO);

	@Mapping(target = "id", source = "attachementId")
	public abstract AttachmentRefOrValueVO map(AttachmentRefOrValue attachmentRefOrValue);

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

	public String map(URL value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	public <C> URI mapGeneric(C value) {
		if (value == null) {
			return null;
		}
		if (value instanceof URI uri) {
			return uri;
		} else if (value instanceof String string) {
			try {
				return new URI(string);
			} catch (URISyntaxException e) {
				throw new MappingException(String.format("String %s is not an URI.", string), e);
			}
		}
		throw new MappingException("Value is not a URI.");
	}

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

	public URI mapToURI(String value) {
		if (value == null) {
			return null;
		}
		return URI.create(value);
	}

	public String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}
}


