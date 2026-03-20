package org.fiware.tmforum.resourceordering;

import io.github.wistefan.mapping.MappingException;
import org.fiware.resourceordering.model.*;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.Note;
import org.fiware.tmforum.resourceordering.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects for the Resource Order Management API.
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public abstract class TMForumMapper extends BaseMapper {

	// resource order

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	@Mapping(target = "orderItem", qualifiedByName = "NullableListMapper")
	public abstract ResourceOrderVO map(ResourceOrderCreateVO resourceOrderCreateVO, URI id);

	public abstract ResourceOrderVO map(ResourceOrder resourceOrder);

	public abstract ResourceOrder map(ResourceOrderVO resourceOrderVO);

	@Mapping(target = "id", source = "id")
	public abstract ResourceOrder map(ResourceOrderUpdateVO resourceOrderUpdateVO, String id);

	/**
	 * Maps a state string from the VO to the domain enum.
	 */
	public ResourceOrderState mapResourceOrderState(String state) {
		if (state == null) {
			return null;
		}
		for (ResourceOrderState s : ResourceOrderState.values()) {
			if (s.getValue().equals(state)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Maps a domain state enum to a VO string.
	 */
	public String mapResourceOrderState(ResourceOrderState state) {
		if (state == null) {
			return null;
		}
		return state.getValue();
	}

	/**
	 * Maps an item state string from the VO to the domain enum.
	 */
	public ResourceOrderItemState mapResourceOrderItemState(String state) {
		if (state == null) {
			return null;
		}
		for (ResourceOrderItemState s : ResourceOrderItemState.values()) {
			if (s.getValue().equals(state)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Maps a domain item state enum to a VO string.
	 */
	public String mapResourceOrderItemState(ResourceOrderItemState state) {
		if (state == null) {
			return null;
		}
		return state.getValue();
	}

	/**
	 * Maps an action string from the VO to the domain enum.
	 */
	public OrderItemAction mapOrderItemAction(String action) {
		if (action == null) {
			return null;
		}
		for (OrderItemAction a : OrderItemAction.values()) {
			if (a.getValue().equals(action)) {
				return a;
			}
		}
		return null;
	}

	/**
	 * Maps a domain action enum to a VO string.
	 */
	public String mapOrderItemAction(OrderItemAction action) {
		if (action == null) {
			return null;
		}
		return action.getValue();
	}

	// cancellation

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract CancelResourceOrderVO map(CancelResourceOrderCreateVO cancelResourceOrderCreateVO, URI id);

	public abstract CancelResourceOrderVO map(CancelResourceOrder cancelResourceOrder);

	public abstract CancelResourceOrder map(CancelResourceOrderVO cancelResourceOrderVO);

	/**
	 * Maps a ResourceOrderRefVO to the domain ResourceOrderRef.
	 */
	public ResourceOrderRef map(ResourceOrderRefVO resourceOrderRefVO) {
		if (resourceOrderRefVO == null) {
			return null;
		}
		return new ResourceOrderRef(resourceOrderRefVO.getId());
	}

	/**
	 * Maps a domain ResourceOrderRef to a ResourceOrderRefVO.
	 */
	public ResourceOrderRefVO map(ResourceOrderRef resourceOrderRef) {
		if (resourceOrderRef == null) {
			return null;
		}
		ResourceOrderRefVO vo = new ResourceOrderRefVO();
		vo.setId(resourceOrderRef.getEntityId().toString());
		if (resourceOrderRef.getHref() != null) {
			vo.setHref(resourceOrderRef.getHref().toString());
		}
		return vo;
	}

	public abstract TaskState map(TaskStateTypeVO taskStateTypeVO);

	public abstract TaskStateTypeVO map(TaskState taskState);

	// order item

	@Mapping(target = "tmfId", source = "id")
	public abstract ResourceOrderItem map(ResourceOrderItemVO resourceOrderItemVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract ResourceOrderItemVO map(ResourceOrderItem resourceOrderItem);

	// subscription

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	// note

	@Mapping(target = "id", source = "tmfId")
	public abstract NoteVO map(Note note);

	@Mapping(target = "tmfId", source = "id")
	public abstract Note map(NoteVO noteVO);

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
