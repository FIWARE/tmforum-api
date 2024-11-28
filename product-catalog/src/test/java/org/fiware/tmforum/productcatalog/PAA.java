package org.fiware.tmforum.productcatalog;

import lombok.Data;
import org.fiware.productcatalog.model.CatalogCreateVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;

@Data
public class PAA  extends ProductOfferingCreateVO {

    private Integer tss;
    private Integer nh4;
    private Integer no3;
}
