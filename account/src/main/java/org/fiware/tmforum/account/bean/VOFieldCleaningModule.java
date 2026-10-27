package org.fiware.tmforum.account.bean;

import jakarta.inject.Singleton;
import org.fiware.account.model.BillFormatVO;
import org.fiware.account.model.BillPresentationMediaVO;
import org.fiware.account.model.BillStructureVO;
import org.fiware.account.model.BillingAccountVO;
import org.fiware.account.model.BillingCycleSpecificationVO;
import org.fiware.account.model.FinancialAccountVO;
import org.fiware.account.model.PartyAccountVO;
import org.fiware.account.model.SettlementAccountVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                BillFormatVO.class,
                BillingAccountVO.class,
                BillingCycleSpecificationVO.class,
                BillPresentationMediaVO.class,
                FinancialAccountVO.class,
                PartyAccountVO.class,
                SettlementAccountVO.class,
                BillStructureVO.class));
    }
}
