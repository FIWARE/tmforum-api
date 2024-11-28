package org.fiware.tmforum.productcatalog;

import lombok.Data;
import org.fiware.productcatalog.model.ProductOfferingVO;

@Data
public class POE extends ProductOfferingVO {

    private Integer tss;
    private Integer nh4;
    private Integer no3;
}
