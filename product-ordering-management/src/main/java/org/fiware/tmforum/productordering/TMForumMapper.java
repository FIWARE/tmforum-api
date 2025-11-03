package org.fiware.tmforum.productordering;

import io.github.wistefan.mapping.MappingException;
import org.fiware.productordering.model.*;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.product.Characteristic;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.ProductSpecificationCharacteristicValueUse;
import org.fiware.tmforum.productordering.domain.*;
import org.fiware.tmforum.resource.Note;
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

	// product

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	@Mapping(target = "productOrderItem", qualifiedByName = "NullableListMapper")
	public abstract ProductOrderVO map(ProductOrderCreateVO productOrderCreateVO, URI id);

	public abstract ProductOrderVO map(ProductOrder productOrder);

	public abstract ProductOrder map(ProductOrderVO productVO);

	@Mapping(target = "id", source = "id")
	public abstract ProductOrder map(ProductOrderUpdateVO productOrderUpdateVO, String id);

	public abstract ProductOrderItemState map(ProductOrderItemStateTypeVO productOrderItemStateTypeVO);

	public abstract ProductOrderItemStateTypeVO map(ProductOrderItemState productOrderItemStateTypeVO);

	public abstract ProductOrderState map(ProductOrderStateTypeVO productOrderStateTypeVO);

	public abstract ProductOrderStateTypeVO map(ProductOrderState productOrderState);

	public abstract OrderItemAction map(OrderItemActionTypeVO orderItemActionTypeVO);

	public abstract OrderItemActionTypeVO map(OrderItemAction orderItemAction);

	// cancellation

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	public abstract CancelProductOrderVO map(CancelProductOrderCreateVO cancelProductOrderVO, URI id);

	public abstract CancelProductOrderVO map(CancelProductOrder cancelProductOrder);

	public abstract CancelProductOrder map(CancelProductOrderVO cancelProductOrderVO);

	public abstract TaskState map(TaskStateTypeVO taskStateTypeVO);

	public abstract TaskStateTypeVO map(TaskState taskState);

	@Mapping(target = "tmfId", source = "id")
	public abstract ProductOrderItem map(ProductOrderItemVO productOrderItemVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract ProductOrderItemVO map(ProductOrderItem productOrderItem);

	@Mapping(target = "query", source = "rawQuery")
	public abstract EventSubscriptionVO map(TMForumSubscription subscription);

	@Mapping(target = "id", source = "tmfId")
	public abstract NoteVO map(Note note);

	@Mapping(target = "tmfId", source = "id")
	public abstract Note map(NoteVO noteVO);

	@Mapping(target = "id", source = "tmfId")
	public abstract QuoteItemRefVO map(QuoteItemRef quoteItemRef);

	@Mapping(target = "tmfId", source = "id")
	public abstract QuoteItemRef map(QuoteItemRefVO quoteItemRef);

	@Mapping(target = "tmfValue", source = "value")
	public abstract Money map(MoneyVO moneyVO);

	@Mapping(target = "value", source = "tmfValue")
	public abstract MoneyVO map(Money money);

	@Mapping(target = "tmfValue", source = "value")
	public abstract Characteristic map(CharacteristicVO characteristicVO);

	@Mapping(target = "value", source = "tmfValue")
	public abstract CharacteristicVO map(Characteristic characteristic);

	public abstract ProductOffering map(ProductOfferingRefVO productOfferingRefVO);

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
