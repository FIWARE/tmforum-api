package org.fiware.tmforum.customerbillmanagement.bean;

import jakarta.inject.Singleton;
import org.fiware.customerbillmanagement.model.AppliedCustomerBillingRateVO;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandVO;
import org.fiware.customerbillmanagement.model.CustomerBillVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                CustomerBillVO.class,
                CustomerBillOnDemandVO.class,
                AppliedCustomerBillingRateVO.class));
    }
}
