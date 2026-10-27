package org.fiware.tmforum.party.bean;

import jakarta.inject.Singleton;
import org.fiware.party.model.IndividualVO;
import org.fiware.party.model.OrganizationVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                IndividualVO.class,
                OrganizationVO.class));
    }
}
