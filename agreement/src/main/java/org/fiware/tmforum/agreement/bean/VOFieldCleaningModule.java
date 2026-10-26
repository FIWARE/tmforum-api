package org.fiware.tmforum.agreement.bean;

import jakarta.inject.Singleton;
import org.fiware.agreement.model.AgreementAuthorizationVO;
import org.fiware.agreement.model.AgreementItemVO;
import org.fiware.agreement.model.AgreementSpecCharacteristicVO;
import org.fiware.agreement.model.AgreementSpecCharacteristicValueVO;
import org.fiware.agreement.model.AgreementSpecificationRefVO;
import org.fiware.agreement.model.AgreementSpecificationRelationshipVO;
import org.fiware.agreement.model.AgreementSpecificationVO;
import org.fiware.agreement.model.AgreementTermOrConditionVO;
import org.fiware.agreement.model.AgreementVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

/**
 * Applies the {@code fields} query parameter to this API's VOs.
 */
@Singleton
public class VOFieldCleaningModule extends FieldCleaningModule {

    public VOFieldCleaningModule() {
        super(List.of(
                AgreementVO.class,
                AgreementSpecificationVO.class,
                AgreementAuthorizationVO.class,
                AgreementItemVO.class,
                AgreementSpecCharacteristicVO.class,
                AgreementSpecCharacteristicValueVO.class,
                AgreementSpecificationRefVO.class,
                AgreementSpecificationRelationshipVO.class,
                AgreementTermOrConditionVO.class));
    }
}
