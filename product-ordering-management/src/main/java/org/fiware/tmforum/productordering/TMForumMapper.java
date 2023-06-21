package org.fiware.tmforum.productordering;

import org.fiware.productordering.model.CancelProductOrderCreateVO;
import org.fiware.productordering.model.CancelProductOrderVO;
import org.fiware.productordering.model.OrderItemActionTypeVO;
import org.fiware.productordering.model.ProductOrderCreateVO;
import org.fiware.productordering.model.ProductOrderItemStateTypeVO;
import org.fiware.productordering.model.ProductOrderStateTypeVO;
import org.fiware.productordering.model.ProductOrderUpdateVO;
import org.fiware.productordering.model.ProductOrderVO;
import org.fiware.productordering.model.TaskStateTypeVO;
import org.fiware.tmforum.common.mapping.IdHelper;
import io.github.wistefan.mapping.MappingException;
import org.fiware.tmforum.productordering.domain.CancelProductOrder;
import org.fiware.tmforum.productordering.domain.OrderItemAction;
import org.fiware.tmforum.productordering.domain.ProductOrder;
import org.fiware.tmforum.productordering.domain.ProductOrderItemState;
import org.fiware.tmforum.productordering.domain.ProductOrderState;
import org.fiware.tmforum.productordering.domain.TaskState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ValueMapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = IdHelper.class)
public interface TMForumMapper {

	// product

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	ProductOrderVO map(ProductOrderCreateVO productOrderCreateVO, URI id);

	ProductOrderVO map(ProductOrder productOrder);

	ProductOrder map(ProductOrderVO productVO);

	@Mapping(target = "id", source = "id")
	ProductOrder map(ProductOrderUpdateVO productOrderUpdateVO, String id);

	@ValueMapping(source = "INPROGRESS", target = "IN_PROGRESS")
	@ValueMapping(source = "ASSESSINGCANCELLATION", target = "ASSESSING_CANCELLATION")
	@ValueMapping(source = "PENDINGCANCELLATION", target = "PENDING_CANCELLATION")
	ProductOrderItemState map(ProductOrderItemStateTypeVO productOrderItemStateTypeVO);

	@ValueMapping(target = "INPROGRESS", source = "IN_PROGRESS")
	@ValueMapping(target = "ASSESSINGCANCELLATION", source = "ASSESSING_CANCELLATION")
	@ValueMapping(target = "PENDINGCANCELLATION", source = "PENDING_CANCELLATION")
	ProductOrderItemStateTypeVO map(ProductOrderItemState productOrderItemStateTypeVO);

	@ValueMapping(source = "INPROGRESS", target = "IN_PROGRESS")
	@ValueMapping(source = "ASSESSINGCANCELLATION", target = "ASSESSING_CANCELLATION")
	@ValueMapping(source = "PENDINGCANCELLATION", target = "PENDING_CANCELLATION")
	ProductOrderState map(ProductOrderStateTypeVO productOrderStateTypeVO);

	@ValueMapping(target = "INPROGRESS", source = "IN_PROGRESS")
	@ValueMapping(target = "ASSESSINGCANCELLATION", source = "ASSESSING_CANCELLATION")
	@ValueMapping(target = "PENDINGCANCELLATION", source = "PENDING_CANCELLATION")
	ProductOrderStateTypeVO map(ProductOrderState productOrderState);

	@ValueMapping(source = "NOCHANGE", target = "NO_CHANGE")
	OrderItemAction map(OrderItemActionTypeVO orderItemActionTypeVO);

	@ValueMapping(target = "NOCHANGE", source = "NO_CHANGE")
	OrderItemActionTypeVO map(OrderItemAction orderItemAction);

	// cancellation

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	CancelProductOrderVO map(CancelProductOrderCreateVO cancelProductOrderVO, URI id);

	CancelProductOrderVO map(CancelProductOrder cancelProductOrder);

	CancelProductOrder map(CancelProductOrderVO cancelProductOrderVO);

	@ValueMapping(source = "INPROGRESS", target = "IN_PROGRESS")
	@ValueMapping(source = "TERMINATEDWITHERROR", target = "TERMINATED_WITH_ERROR")
	TaskState map(TaskStateTypeVO taskStateTypeVO);

	@ValueMapping(target = "TERMINATEDWITHERROR", source = "TERMINATED_WITH_ERROR")
	@ValueMapping(target = "INPROGRESS", source = "IN_PROGRESS")
	TaskStateTypeVO map(TaskState taskState);

	default URL map(String value) {
		if (value == null) {
			return null;
		}
		try {
			return new URL(value);
		} catch (MalformedURLException e) {
			throw new MappingException(String.format("%s is not a URL.", value), e);
		}
	}

	default String map(URL value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}

	default URI mapToURI(String value) {
		if (value == null) {
			return null;
		}
		return URI.create(value);
	}

	default String mapFromURI(URI value) {
		if (value == null) {
			return null;
		}
		return value.toString();
	}
}


