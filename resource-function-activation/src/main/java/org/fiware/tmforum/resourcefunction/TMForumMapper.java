package org.fiware.tmforum.resourcefunction;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourcefunction.model.*;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.*;
import org.fiware.tmforum.resourcefunction.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public abstract class TMForumMapper extends BaseMapper {

	// resource function

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ResourceFunctionVO map(ResourceFunctionCreateVO resourceFunctionCreateVO, URI id);

	public abstract ResourceFunctionVO map(ResourceFunction resourceFunction);

	public abstract ResourceFunction map(ResourceFunctionVO resourceFunctionVO);

	@Mapping(target = "id", source = "id")
	public abstract ResourceFunction map(ResourceFunctionUpdateVO resourceFunctionUpdateVO, String id);

	// monitor

	public abstract MonitorVO map(Monitor monitor);

	// heal

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract HealVO map(HealCreateVO healCreateVO, URI id);

	public abstract HealVO map(Heal heal);

	public abstract Heal map(HealVO healVO);

	// migrate

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract MigrateVO map(MigrateCreateVO migrateCreateVO, URI id);

	public abstract MigrateVO map(Migrate migrate);

	public abstract Migrate map(MigrateVO migrateVO);

	// scale

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ScaleVO map(ScaleCreateVO scaleCreateVOVO, URI id);

	public abstract ScaleVO map(Scale scale);

	public abstract Scale map(ScaleVO scaleVO);

	// sub-entities

	public abstract Resource map(ResourceRefOrValueVO resourceRefOrValueVO);

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

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

	@Mapping(target = "graphId", source = "id")
	public abstract ResourceGraph map(ResourceGraphVO resourceGraphVO);

	@Mapping(target = "id", source = "graphId")
	public abstract ResourceGraphVO map(ResourceGraph resourceGraph);

	@Mapping(target = "graphRelationId", source = "id")
	public abstract ResourceGraphRelationship map(ResourceGraphRelationshipVO resourceGraphRelationshipVO);

	@Mapping(target = "id", source = "graphRelationId")
	public abstract ResourceGraphRelationshipVO map(ResourceGraphRelationship resourceGraphRelationship);

	@Mapping(target = "connectionId", source = "id")
	public abstract Connection map(ConnectionVO connectionVO);

	@Mapping(target = "id", source = "connectionId")
	public abstract ConnectionVO map(Connection connection);

	@Mapping(target = "attachementId", source = "id")
	public abstract AttachmentRefOrValue map(AttachmentRefOrValueVO attachmentRefOrValueVO);

	@Mapping(target = "id", source = "attachementId")
	public abstract AttachmentRefOrValueVO map(AttachmentRefOrValue attachmentRefOrValue);

	@Mapping(target = "id", source = "noteId")
	public abstract NoteVO map(Note note);

	@Mapping(target = "noteId", source = "id")
	public abstract Note map(NoteVO noteVO);

	@Mapping(target = "id", source = "featureId")
	public abstract FeatureVO map(Feature feature);

	@Mapping(target = "featureId", source = "id")
	public abstract Feature map(FeatureVO featureVO);

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


