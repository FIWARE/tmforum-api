package org.fiware.tmforum.serviceinventory;

import org.fiware.ngsi.model.SubscriptionVO;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.resource.*;
import io.github.wistefan.mapping.MappingException;
import org.fiware.serviceinventory.model.*;
import org.fiware.tmforum.serviceinventory.domain.*;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.ResourceSpecificationRef;
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

	// product

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract ServiceVO map(ServiceCreateVO productCreateVO, URI id);

	public abstract ServiceVO map(Service product);

	public abstract Service map(ServiceVO productVO);

	@Mapping(target = "id", source = "id")
	public abstract Service map(ServiceUpdateVO productUpdateVO, String id);

	@Mapping(target = "id", source = "tmfId")
	public abstract NoteVO map(Note note);

	@Mapping(target = "tmfId", source = "id")
	public abstract Note map(NoteVO noteVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract FeatureVO map(Feature feature);

	@Mapping(target = "tmfId", source = "id")
	public abstract Feature map(FeatureVO featureVO);

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

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

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
}
