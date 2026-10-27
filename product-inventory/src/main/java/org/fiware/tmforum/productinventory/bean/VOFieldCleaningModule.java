package org.fiware.tmforum.productinventory.bean;

import jakarta.inject.Singleton;
import org.fiware.productinventory.model.ProductVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                ProductVO.class));
    }
}
