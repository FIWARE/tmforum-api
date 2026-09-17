package org.fiware.tmforum.resourcefunction;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourcefunction.model.AttachmentRefOrValueVO;
import org.fiware.resourcefunction.model.CharacteristicRelationshipVO;
import org.fiware.resourcefunction.model.CharacteristicVO;
import org.fiware.resourcefunction.model.ConnectionVO;
import org.fiware.resourcefunction.model.EventSubscriptionVO;
import org.fiware.resourcefunction.model.FeatureVO;
import org.fiware.resourcefunction.model.HealCreateVO;
import org.fiware.resourcefunction.model.HealVO;
import org.fiware.resourcefunction.model.MigrateCreateVO;
import org.fiware.resourcefunction.model.MigrateVO;
import org.fiware.resourcefunction.model.MonitorVO;
import org.fiware.resourcefunction.model.NoteVO;
import org.fiware.resourcefunction.model.ResourceFunctionCreateVO;
import org.fiware.resourcefunction.model.ResourceFunctionUpdateVO;
import org.fiware.resourcefunction.model.ResourceFunctionVO;
import org.fiware.resourcefunction.model.ResourceGraphRelationshipVO;
import org.fiware.resourcefunction.model.ResourceGraphVO;
import org.fiware.resourcefunction.model.ResourceRefOrValueVO;
import org.fiware.resourcefunction.model.ScaleCreateVO;
import org.fiware.resourcefunction.model.ScaleVO;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.Characteristic;
import org.fiware.tmforum.resource.CharacteristicRelationship;
import org.fiware.tmforum.resource.Feature;
import org.fiware.tmforum.resource.Note;
import org.fiware.tmforum.resource.Resource;
import org.fiware.tmforum.resourcefunction.domain.Connection;
import org.fiware.tmforum.resourcefunction.domain.Heal;
import org.fiware.tmforum.resourcefunction.domain.Migrate;
import org.fiware.tmforum.resourcefunction.domain.Monitor;
import org.fiware.tmforum.resourcefunction.domain.ResourceFunction;
import org.fiware.tmforum.resourcefunction.domain.ResourceGraph;
import org.fiware.tmforum.resourcefunction.domain.ResourceGraphRelationship;
import org.fiware.tmforum.resourcefunction.domain.Scale;
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

	@Mapping(target = "tmfValue", source = "value")
	@Mapping(target = "tmfId", source = "id")
	public abstract Characteristic map(CharacteristicVO characteristicVO);

	@Mapping(target = "value", source = "tmfValue")
	@Mapping(target = "id", source = "tmfId")
	public abstract CharacteristicVO map(Characteristic characteristic);

	@Mapping(target = "tmfId", source = "id")
	public abstract CharacteristicRelationship map(CharacteristicRelationshipVO characteristicRelationshipVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract CharacteristicRelationshipVO map(CharacteristicRelationship characteristicRelationship);

	@Mapping(target = "tmfId", source = "id")
	public abstract ResourceGraph map(ResourceGraphVO resourceGraphVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract ResourceGraphVO map(ResourceGraph resourceGraph);

	@Mapping(target = "tmfId", source = "id")
	public abstract ResourceGraphRelationship map(ResourceGraphRelationshipVO resourceGraphRelationshipVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract ResourceGraphRelationshipVO map(ResourceGraphRelationship resourceGraphRelationship);

	@Mapping(target = "tmfId", source = "id")
	public abstract Connection map(ConnectionVO connectionVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract ConnectionVO map(Connection connection);

	@Mapping(target = "tmfId", source = "id")
	public abstract AttachmentRefOrValue map(AttachmentRefOrValueVO attachmentRefOrValueVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract AttachmentRefOrValueVO map(AttachmentRefOrValue attachmentRefOrValue);

	@Mapping(target = "id", source = "tmfId")
	public abstract NoteVO map(Note note);

	@Mapping(target = "tmfId", source = "id")
	public abstract Note map(NoteVO noteVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureVO map(Feature feature);

	@Mapping(target = "tmfId", source = "id")
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


