package org.fiware.tmforum.usagemanagement.domain;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.product.ProductRef;

@Data
@EqualsAndHashCode(callSuper = true)
public class RatedProductUsage extends Entity {
    private boolean isBilled;
    private boolean isTaxExempt;
    private String offerTariffType;
    private String ratingAmountType;
    private Instant ratingDate;
    private float taxRate;
    private String usageRatingTag;
    private Money bucketValueConvertedInAmount;
    private ProductRef productRef;
    private Money taxExcludedRatingAmount;
    private Money taxIncludedRatingAmount;
}