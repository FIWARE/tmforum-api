package org.fiware.tmforum.productcatalog;

import io.github.wistefan.mapping.MappingException;
import org.fiware.productcatalog.model.*;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.ProductOfferingPrice;
import org.fiware.tmforum.product.ProductSpecification;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.mapstruct.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Mapper between the internal model and api-domain objects
 */
@Mapper(componentModel = "jsr330", uses = {IdHelper.class, MappingHelper.class})
public abstract class TMForumMapper extends BaseMapper {

    // catalog

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract CatalogVO map(CatalogCreateVO catalogCreateVO, URI id);

    public abstract CatalogVO map(Catalog catalog);

    @Mapping(target = "href", source = "id")
    public abstract Catalog map(CatalogVO catalogVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract CatalogVO map(CatalogUpdateVO catalogUpdateVO, String id);

    // category

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract CategoryVO map(CategoryCreateVO categoryCreateVO, URI id);

    @Mapping(target = "parentId", qualifiedByName = "fromCategoryRef")
    public abstract CategoryVO map(Category category);

    @Mapping(target = "href", source = "id")
    @Mapping(target = "parentId", source = "categoryVO.parentId", qualifiedByName = "toCategoryRef")
    public abstract Category map(CategoryVO categoryVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract CategoryVO map(CategoryUpdateVO categoryUpdateVO, String id);

    // product offering

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract ProductOfferingVO map(ProductOfferingCreateVO productOfferingCreateVO, URI id);

    public abstract ProductOfferingVO map(ProductOffering productOfferingVO);

    @Mapping(target = "href", source = "id")
    public abstract ProductOffering map(ProductOfferingVO productOfferingVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract ProductOfferingVO map(ProductOfferingUpdateVO productOfferingUpdateVO, String id);

    // product offering price

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract ProductOfferingPriceVO map(ProductOfferingPriceCreateVO productOfferingPriceCreateVO, URI id);

    public abstract ProductOfferingPriceVO map(ProductOfferingPrice productOfferingPrice);

    @Mapping(target = "href", source = "id")
    public abstract ProductOfferingPrice map(ProductOfferingPriceVO productOfferingPriceVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract ProductOfferingPriceVO map(ProductOfferingPriceUpdateVO productOfferingPriceUpdateVO, String id);

    // product specification

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract ProductSpecificationVO map(ProductSpecificationCreateVO productSpecificationCreateVO, URI id);

    public abstract ProductSpecificationVO map(ProductSpecification productSpecification);

    @Mapping(target = "href", source = "id")
    public abstract ProductSpecification map(ProductSpecificationVO productSpecificationVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract ProductSpecificationVO map(ProductSpecificationUpdateVO productSpecificationUpdateVO, String id);

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


