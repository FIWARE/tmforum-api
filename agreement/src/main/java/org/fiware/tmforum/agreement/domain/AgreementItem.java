package org.fiware.tmforum.agreement.domain;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.product.ProductOfferingRef;
import org.fiware.tmforum.product.ProductRef;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgreementItem extends Entity {
    private List<ProductRef> product;
    private List<ProductOfferingRef> productOffering;
    private List<AgreementTermOrCondition> termOrCondition;

}
