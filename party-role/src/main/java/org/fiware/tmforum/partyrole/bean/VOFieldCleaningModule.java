package org.fiware.tmforum.partyrole.bean;

import jakarta.inject.Singleton;
import org.fiware.partyRole.model.AccountRefVO;
import org.fiware.partyRole.model.AgreementRefVO;
import org.fiware.partyRole.model.CreditProfileVO;
import org.fiware.partyRole.model.PartyRoleVO;
import org.fiware.partyRole.model.PaymentMethodRefVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                PartyRoleVO.class,
                AccountRefVO.class,
                CreditProfileVO.class,
                AgreementRefVO.class,
                PaymentMethodRefVO.class));
    }
}
