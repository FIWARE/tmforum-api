package org.fiware.tmforum.resourcecatalog.bean;

import jakarta.inject.Singleton;
import org.fiware.resourcecatalog.model.ResourceCandidateVO;
import org.fiware.resourcecatalog.model.ResourceCatalogVO;
import org.fiware.resourcecatalog.model.ResourceCategoryVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                ResourceSpecificationVO.class,
                ResourceCategoryVO.class,
                ResourceCatalogVO.class,
                ResourceCandidateVO.class));
    }
}
