package org.fiware.tmforum.usagemanagement.bean;

import jakarta.inject.Singleton;
import org.fiware.usagemanagement.model.RatedProductUsageVO;
import org.fiware.usagemanagement.model.UsageCharacteristicVO;
import org.fiware.usagemanagement.model.UsageSpecificationVO;
import org.fiware.usagemanagement.model.UsageStatusTypeVO;
import org.fiware.usagemanagement.model.UsageVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                RatedProductUsageVO.class,
                UsageVO.class,
                UsageCharacteristicVO.class,
                UsageSpecificationVO.class,
                UsageStatusTypeVO.class));
    }
}
