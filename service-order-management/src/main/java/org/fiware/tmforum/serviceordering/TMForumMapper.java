package org.fiware.tmforum.serviceordering;

import io.github.wistefan.mapping.MappingException;
import org.fiware.serviceordering.model.*;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.resource.Note;
import org.fiware.tmforum.serviceordering.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects for the Service Order Management API.
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public abstract class TMForumMapper extends BaseMapper {

	// service order

	/**
	 * Maps a service order create VO to a service order VO with the given id.
	 *
	 * @param serviceOrderCreateVO the create VO to map
	 * @param id                   the NGSI-LD id to assign
	 * @return the mapped service order VO
	 */
	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	@Mapping(target = "serviceOrderItem", qualifiedByName = "NullableListMapper")
	public abstract ServiceOrderVO map(ServiceOrderCreateVO serviceOrderCreateVO, URI id);

	/**
	 * Maps a service order domain object to its VO representation.
	 *
	 * @param serviceOrder the domain object to map
	 * @return the mapped VO
	 */
	public abstract ServiceOrderVO map(ServiceOrder serviceOrder);

	/**
	 * Maps a service order VO to its domain representation.
	 *
	 * @param serviceOrderVO the VO to map
	 * @return the mapped domain object
	 */
	public abstract ServiceOrder map(ServiceOrderVO serviceOrderVO);

	/**
	 * Maps a service order update VO to a domain object with the given id.
	 *
	 * @param serviceOrderUpdateVO the update VO to map
	 * @param id                   the NGSI-LD id of the service order to update
	 * @return the mapped domain object
	 */
	@Mapping(target = "id", source = "id")
	@Mapping(target = "atSchemaLocation", ignore = true)
	public abstract ServiceOrder map(ServiceOrderUpdateVO serviceOrderUpdateVO, String id);

	/**
	 * Maps a ServiceOrderStateTypeVO to the domain ServiceOrderState enum.
	 *
	 * @param stateTypeVO the VO state to map
	 * @return the corresponding domain state
	 */
	public abstract ServiceOrderState map(ServiceOrderStateTypeVO stateTypeVO);

	/**
	 * Maps a domain ServiceOrderState to its VO representation.
	 *
	 * @param state the domain state to map
	 * @return the corresponding VO state
	 */
	public abstract ServiceOrderStateTypeVO map(ServiceOrderState state);

	/**
	 * Maps a ServiceOrderItemStateTypeVO to the domain ServiceOrderItemState enum.
	 *
	 * @param stateTypeVO the VO item state to map
	 * @return the corresponding domain item state
	 */
	public abstract ServiceOrderItemState map(ServiceOrderItemStateTypeVO stateTypeVO);

	/**
	 * Maps a domain ServiceOrderItemState to its VO representation.
	 *
	 * @param state the domain item state to map
	 * @return the corresponding VO item state
	 */
	public abstract ServiceOrderItemStateTypeVO map(ServiceOrderItemState state);

	/**
	 * Maps an OrderItemActionTypeVO to the domain OrderItemAction enum.
	 *
	 * @param actionTypeVO the VO action to map
	 * @return the corresponding domain action
	 */
	public abstract OrderItemAction map(OrderItemActionTypeVO actionTypeVO);

	/**
	 * Maps a domain OrderItemAction to its VO representation.
	 *
	 * @param action the domain action to map
	 * @return the corresponding VO action
	 */
	public abstract OrderItemActionTypeVO map(OrderItemAction action);

	// cancellation

	/**
	 * Maps a cancel service order create VO to a cancel service order VO with the given id.
	 *
	 * @param cancelServiceOrderCreateVO the create VO to map
	 * @param id                         the NGSI-LD id to assign
	 * @return the mapped cancel service order VO
	 */
	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract CancelServiceOrderVO map(CancelServiceOrderCreateVO cancelServiceOrderCreateVO, URI id);

	/**
	 * Maps a cancel service order domain object to its VO representation.
	 *
	 * @param cancelServiceOrder the domain object to map
	 * @return the mapped VO
	 */
	public abstract CancelServiceOrderVO map(CancelServiceOrder cancelServiceOrder);

	/**
	 * Maps a cancel service order VO to its domain representation.
	 *
	 * @param cancelServiceOrderVO the VO to map
	 * @return the mapped domain object
	 */
	public abstract CancelServiceOrder map(CancelServiceOrderVO cancelServiceOrderVO);

	/**
	 * Maps a ServiceOrderRefVO to the domain ServiceOrderRef.
	 *
	 * @param serviceOrderRefVO the VO to map
	 * @return the mapped domain reference
	 */
	public ServiceOrderRef map(ServiceOrderRefVO serviceOrderRefVO) {
		if (serviceOrderRefVO == null) {
			return null;
		}
		return new ServiceOrderRef(serviceOrderRefVO.getId());
	}

	/**
	 * Maps a domain ServiceOrderRef to a ServiceOrderRefVO.
	 *
	 * @param serviceOrderRef the domain reference to map
	 * @return the mapped VO
	 */
	public ServiceOrderRefVO map(ServiceOrderRef serviceOrderRef) {
		if (serviceOrderRef == null) {
			return null;
		}
		ServiceOrderRefVO vo = new ServiceOrderRefVO();
		vo.setId(serviceOrderRef.getEntityId().toString());
		vo.setHref(serviceOrderRef.getHref());
		return vo;
	}

	/**
	 * Maps a TaskStateTypeVO to the domain TaskState enum.
	 *
	 * @param taskStateTypeVO the VO task state to map
	 * @return the corresponding domain task state
	 */
	public abstract TaskState map(TaskStateTypeVO taskStateTypeVO);

	/**
	 * Maps a domain TaskState to its VO representation.
	 *
	 * @param taskState the domain task state to map
	 * @return the corresponding VO task state
	 */
	public abstract TaskStateTypeVO map(TaskState taskState);

	// order item

	/**
	 * Maps a ServiceOrderItemVO to the domain ServiceOrderItem, mapping id to tmfId.
	 *
	 * @param serviceOrderItemVO the VO to map
	 * @return the mapped domain object
	 */
	@Mapping(target = "tmfId", source = "id")
	public abstract ServiceOrderItem map(ServiceOrderItemVO serviceOrderItemVO);

	/**
	 * Maps a domain ServiceOrderItem to its VO representation, mapping tmfId to id.
	 *
	 * @param serviceOrderItem the domain object to map
	 * @return the mapped VO
	 */
	@Mapping(target = "id", source = "tmfId")
	public abstract ServiceOrderItemVO map(ServiceOrderItem serviceOrderItem);

	// subscription

	/**
	 * Maps a TMForumSubscription to an EventSubscriptionVO.
	 *
	 * @param subscription the subscription to map
	 * @return the mapped VO
	 */
	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	// note

	/**
	 * Maps a Note domain object to a NoteVO.
	 *
	 * @param note the domain note to map
	 * @return the mapped VO
	 */
	@Mapping(target = "id", source = "tmfId")
	public abstract NoteVO map(Note note);

	/**
	 * Maps a NoteVO to a Note domain object.
	 *
	 * @param noteVO the VO to map
	 * @return the mapped domain note
	 */
	@Mapping(target = "tmfId", source = "id")
	public abstract Note map(NoteVO noteVO);

	/**
	 * Converts a string to a URL.
	 *
	 * @param value the string to convert
	 * @return the resulting URL
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
	 * Converts a URL to a string.
	 *
	 * @param value the URL to convert
	 * @return the resulting string
	 */
	public String map(URL value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	/**
	 * Converts a string to a URI.
	 *
	 * @param value the string to convert
	 * @return the resulting URI
	 */
	public URI mapToURI(String value) {
		if (value == null) {
			return null;
		}
		return URI.create(value);
	}

	/**
	 * Converts a URI to a string.
	 *
	 * @param value the URI to convert
	 * @return the resulting string
	 */
	public String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}
}
