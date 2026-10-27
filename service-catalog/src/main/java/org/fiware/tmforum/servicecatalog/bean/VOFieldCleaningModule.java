package org.fiware.tmforum.servicecatalog.bean;

import jakarta.inject.Singleton;
import org.fiware.servicecatalog.model.ServiceCandidateVO;
import org.fiware.servicecatalog.model.ServiceCatalogVO;
import org.fiware.servicecatalog.model.ServiceCategoryVO;
import org.fiware.servicecatalog.model.ServiceSpecificationVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                ServiceCandidateVO.class,
                ServiceCatalogVO.class,
                ServiceCategoryVO.class,
                ServiceSpecificationVO.class));
    }
}
