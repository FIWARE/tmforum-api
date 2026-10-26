package org.fiware.tmforum.quote.bean;

import jakarta.inject.Singleton;
import org.fiware.quote.model.QuoteVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                QuoteVO.class));
    }
}
