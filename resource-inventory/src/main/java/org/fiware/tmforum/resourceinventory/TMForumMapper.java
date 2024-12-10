package org.fiware.tmforum.resourceinventory;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourceinventory.model.EventSubscriptionVO;
import org.fiware.resourceinventory.model.ResourceCreateVO;
import org.fiware.resourceinventory.model.ResourceUpdateVO;
import org.fiware.resourceinventory.model.ResourceVO;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.Resource;
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


