package org.fiware.tmforum.productcatalog.bean;

import jakarta.inject.Singleton;
import org.fiware.productcatalog.model.CatalogVO;
import org.fiware.productcatalog.model.CategoryVO;
import org.fiware.productcatalog.model.ProductOfferingPriceVO;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.productcatalog.model.ProductSpecificationVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                ProductOfferingPriceVO.class,
                CategoryVO.class,
                CatalogVO.class,
                ProductSpecificationVO.class,
                ProductOfferingVO.class));
    }
}
