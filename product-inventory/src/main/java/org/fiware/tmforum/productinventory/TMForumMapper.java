package org.fiware.tmforum.productinventory;

import io.github.wistefan.mapping.MappingException;
import org.fiware.productinventory.model.*;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.product.RelatedProductOrderItemRef;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
	ProductVO map(ProductCreateVO productCreateVO, URI id);

	ProductVO map(Product product);

	Product map(ProductVO productVO);

	@Mapping(target = "id", source = "id")
	Product map(ProductUpdateVO productUpdateVO, String id);

	RelatedProductOrderItemRef map (RelatedProductOrderItemVO relatedProductOrderItemVO);

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

	@Mapping(target = "query", source = "rawQuery")
	EventSubscriptionVO map(TMForumSubscription subscription);

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


