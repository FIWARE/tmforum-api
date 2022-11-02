package org.fiware.tmforum.productcatalog;

import org.fiware.tmforum.product.CategoryRef;
import org.mapstruct.Named;

import java.net.URI;
import java.util.Optional;

@Named("MappingHelper")
public class MappingHelper {


    @Named("toCategoryRef")
    public static CategoryRef toCategoryRef(String parentId) {
        if (parentId == null) {
            return null;
        }
        return new CategoryRef(parentId);
    }

    @Named("fromCategoryRef")
    public static String fromCategoryRef(CategoryRef parentId) {
        if (parentId == null) {
            return null;
        }
        return Optional.ofNullable(parentId.getId()).map(URI::toString).orElse(null);
    }
}
