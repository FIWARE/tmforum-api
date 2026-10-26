package org.fiware.tmforum.serviceinventory.bean;

import jakarta.inject.Singleton;
import org.fiware.serviceinventory.model.ServiceVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                ServiceVO.class));
    }
}
