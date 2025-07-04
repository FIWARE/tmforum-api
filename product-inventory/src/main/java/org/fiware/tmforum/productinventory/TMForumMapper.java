package org.fiware.tmforum.productinventory;

import io.github.wistefan.mapping.MappingException;
import org.fiware.productinventory.model.*;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.product.Characteristic;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.product.RelatedProductOrderItemRef;
import org.fiware.tmforum.service.CharacteristicValueSpecification;
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
	public abstract ProductVO map(ProductCreateVO productCreateVO, URI id);

	public abstract ProductVO map(Product product);

	public abstract Product map(ProductVO productVO);

	@Mapping(target = "id", source = "id")
	public abstract Product map(ProductUpdateVO productUpdateVO, String id);

	@Mapping(target = "charValue", source = "value")
	public abstract Characteristic map(CharacteristicVO characteristicVO);

	@Mapping(target = "value", source = "charValue")
	public abstract CharacteristicVO map(Characteristic characteristic);

	public abstract RelatedProductOrderItemRef map(RelatedProductOrderItemVO relatedProductOrderItemVO);

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
}


