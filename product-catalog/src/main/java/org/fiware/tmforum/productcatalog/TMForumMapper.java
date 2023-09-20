package org.fiware.tmforum.productcatalog;

import io.github.wistefan.mapping.MappingException;
import org.fiware.productcatalog.model.*;
import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.ProductOfferingPrice;
import org.fiware.tmforum.product.ProductSpecification;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = { IdHelper.class, MappingHelper.class })
public interface TMForumMapper {

	// catalog

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	CatalogVO map(CatalogCreateVO catalogCreateVO, URI id);

	CatalogVO map(Catalog catalog);

	@Mapping(target = "href", source = "id")
	Catalog map(CatalogVO catalogVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	CatalogVO map(CatalogUpdateVO catalogUpdateVO, String id);

	// category

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	CategoryVO map(CategoryCreateVO categoryCreateVO, URI id);

	@Mapping(target = "parentId", qualifiedByName = "fromCategoryRef")
	CategoryVO map(Category category);

	@Mapping(target = "href", source = "id")
	@Mapping(target = "parentId", source = "categoryVO.parentId", qualifiedByName = "toCategoryRef")
	Category map(CategoryVO categoryVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	CategoryVO map(CategoryUpdateVO categoryUpdateVO, String id);

	// product offering

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	ProductOfferingVO map(ProductOfferingCreateVO productOfferingCreateVO, URI id);

	ProductOfferingVO map(ProductOffering productOfferingVO);

	@Mapping(target = "href", source = "id")
	ProductOffering map(ProductOfferingVO productOfferingVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	ProductOfferingVO map(ProductOfferingUpdateVO productOfferingUpdateVO, String id);

	// product offering price

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	ProductOfferingPriceVO map(ProductOfferingPriceCreateVO productOfferingPriceCreateVO, URI id);

	ProductOfferingPriceVO map(ProductOfferingPrice productOfferingPrice);

	@Mapping(target = "href", source = "id")
	ProductOfferingPrice map(ProductOfferingPriceVO productOfferingPriceVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	ProductOfferingPriceVO map(ProductOfferingPriceUpdateVO productOfferingPriceUpdateVO, String id);

	// product specification

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	ProductSpecificationVO map(ProductSpecificationCreateVO productSpecificationCreateVO, URI id);

	ProductSpecificationVO map(ProductSpecification productSpecification);

	@Mapping(target = "href", source = "id")
	ProductSpecification map(ProductSpecificationVO productSpecificationVO);

	@Mapping(target = "id", source = "id")
	@Mapping(target = "href", source = "id")
	ProductSpecificationVO map(ProductSpecificationUpdateVO productSpecificationUpdateVO, String id);

	@Mapping(target = "query", source = "rawQuery")
	EventSubscriptionVO map(Subscription subscription);

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


